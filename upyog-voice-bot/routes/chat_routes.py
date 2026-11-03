import os
import re
import json
import logging
import threading
from typing import Dict, Any, Optional
from flask import Blueprint, request, jsonify, Response
from dotenv import load_dotenv

try:
    from groq import Groq
except ImportError:
    Groq = None

load_dotenv()

logger = logging.getLogger(__name__)

chat_bp = Blueprint("chat_routes", __name__)

GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
GROQ_MODEL = os.environ.get("GROQ_MODEL", "openai/gpt-oss-20b")
groq_client = None

# Import modular handlers from chat_handlers
from services.chat_handlers import (
    resolve_auth_and_profile,
    dispatch_chat_request,
    build_chat_error_response
)


# Summarizes a block of older chat messages and archives them in Qdrant long-term memory
def summarize_and_store_memory(phone_anchor, messages_to_summarize):
    global groq_client
    try:
        from storage.qdrant_manager import MemoryManager
        from services.rag_service import model

        convo_text = ""
        for msg in messages_to_summarize:
            role = "User" if msg.get("role") == "user" else "Assistant"
            convo_text += f"{role}: {msg.get('content')}\n"

        if not groq_client and Groq:
            groq_client = Groq(api_key=GROQ_API_KEY)

        from prompts.memory_prompts import build_memory_summary_prompt
        prompt = build_memory_summary_prompt(convo_text=convo_text)

        if groq_client:
            response = groq_client.chat.completions.create(
                messages=[{"role": "user", "content": prompt}],
                model=GROQ_MODEL,
                max_tokens=400,
                temperature=0.3
            )
            summary_text = response.choices[0].message.content.strip()

            embedding = model.encode([summary_text])[0].tolist()

            success = MemoryManager.save_long_term_interaction(
                phone_number=phone_anchor,
                role="system_summary",
                content=summary_text,
                embedding=embedding
            )
            if success:
                logger.info(f"Successfully summarized and stored memory for {phone_anchor}")
    except Exception as e:
        logger.error(f"Error in summarize_and_store_memory: {e}")


"""
Main chat endpoint — three route aliases registered:
  /chat                  → direct local access (localhost:8090)
  /upyog-voice-bot/chat  → production via niautt EKS ingress
  /upyog-voice/chat      → backward compatibility with old deployment path
GET requests return a health check response for Kubernetes liveness probes.
"""
@chat_bp.route("/chat", methods=["GET", "POST"])
@chat_bp.route("/upyog-voice-bot/chat", methods=["GET", "POST"])
@chat_bp.route("/upyog-voice/chat", methods=["GET", "POST"])
def chat():
    if request.method == "GET":
        logger.info("[ENDPOINT /chat GET] Health check ping")
        return jsonify({"status": "ok", "message": "UPYOG Voice Bot Chat Endpoint"}), 200

    import services.rag_service as rag_service
    from services.rag_service import is_hard_blocked
    from services.voice_service import text_to_speech
    from services.intent_service import (
        detect_language, _SERVICES_REGISTRY, _SYSTEM_CONFIG, workflows
    )
    from storage.redis_manager import get_chat_history

    try:
        # 1. Warm-up wait for RAG models
        if not rag_service.resources_ready_event.is_set() or any(x is None for x in [rag_service.model, rag_service.data, rag_service.index]):
            logger.warning("[ENDPOINT /chat POST] Resources still warming up — waiting up to 10s...")
            rag_service.resources_ready_event.wait(timeout=10)
            if any(x is None for x in [rag_service.model, rag_service.data, rag_service.index]):
                rag_service.load_resources()
            if any(x is None for x in [rag_service.model, rag_service.data, rag_service.index]):
                logger.error("[ENDPOINT /chat POST] Resources unavailable (503 Service Unavailable)")
                return jsonify({"error": "Loading resources..."}), 503

        # 2. Extract request parameters
        user_data = request.json or {}
        user_input = user_data.get("query") or user_data.get("user_input") or ""
        session_id = user_data.get("session_id", "default")
        request_info = user_data.get("request_info") or user_data.get("RequestInfo", {})
        file_name = user_data.get("file_name")
        file_data = user_data.get("file_data")

        # 3. Dynamic Authentication & Identity Resolution
        phone_anchor, token, cached_info, is_authenticated = resolve_auth_and_profile(session_id, request_info, user_data)
        history_key = phone_anchor if (phone_anchor and phone_anchor != "default") else session_id
        history = get_chat_history(history_key) if history_key else []
        if not history and user_data.get("history"):
            raw_hist = user_data.get("history")
            if isinstance(raw_hist, list):
                history = [h for h in raw_hist if isinstance(h, dict) and "role" in h and "content" in h]

        # 4. Handle Document Uploads if attached 
        if file_name and file_data and token:
            from mcp_tools import upload_to_filestore
            file_store_id = upload_to_filestore(file_name, file_data, token)
            if file_store_id:
                user_input = json.dumps({"document": file_store_id})
                file_name = None

        if file_name and not user_input:
            user_input = json.dumps({"document": file_name})

        if not user_input and not file_name:
            return jsonify({"response": "", "lang": "en", "audio": ""})

        # 5. Language detection
        lang_info = detect_language(user_input)
        user_language = lang_info['lang'] 
        detected_script = lang_info['script']

        logger.info(f"━━━ REQUEST [Session: {session_id}] ━━━")
        logger.info(f"Query: '{user_input}' | Lang: {user_language} | Script: {detected_script}")
        logger.info(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        # 6. Hard-block check for out-of-domain topics
        if is_hard_blocked(user_input):
            logger.info(f"[CHAT FLOW] Query hard-blocked for out-of-domain topic.")
            sys_blocked = _SYSTEM_CONFIG.get("system_messages", {}).get("blocked", {})
            msg = (sys_blocked.get("response_hi", "मैं केवल UPYOG और शहरी सरकारी सेवाओं के बारे में सहायता कर सकता हूँ।")
                   if user_language == 'hi' else
                   sys_blocked.get("response_en", "I can only help with UPYOG and urban government services."))
            audio_output = text_to_speech(msg, user_language)
            return jsonify({"response": msg, "lang": user_language, "audio": audio_output, "mode": "blocked"})

        # 7. Unified Intent & Workflow Execution Dispatcher
        chat_resp = dispatch_chat_request(
            user_input=user_input,
            user_language=user_language,
            detected_script=detected_script,
            history=history,
            session_id=session_id,
            phone_anchor=phone_anchor,
            is_authenticated=is_authenticated,
            cached_info=cached_info,
            workflows=workflows,
            services_registry=_SERVICES_REGISTRY,
            system_config=_SYSTEM_CONFIG
        )
        return jsonify(chat_resp), 200

    except Exception as e:
        logger.error(f"[ENDPOINT /chat ERROR] Exception: {e}", exc_info=True)
        err_lang = user_language if 'user_language' in locals() else "en"
        err_resp = build_chat_error_response(e, err_lang)
        return jsonify(err_resp), 200


@chat_bp.after_app_request
def log_chat_to_redis(response):
    if not request.path.endswith("/chat"):
        return response

    try:
        if response.status_code == 200:
            req_data = request.get_json(silent=True) or {}
            session_id = req_data.get("session_id")
            user_input = req_data.get("query", "").strip()

            file_name = req_data.get("file_name")
            if not user_input and file_name:
                user_input = json.dumps({"document": file_name})

            res_data = response.get_json(silent=True) or {}
            response_text = res_data.get("response", "").strip()

            if session_id and user_input and response_text:
                from services.user_service import extract_phone_from_session
                phone_anchor = extract_phone_from_session(session_id)
                storage_key = phone_anchor if (phone_anchor and phone_anchor != "default") else session_id
                if storage_key:
                    from storage.redis_manager import get_chat_history, save_chat_history
                    redis_chat = get_chat_history(storage_key) or []
                    if not redis_chat or redis_chat[-1].get("content") != response_text or (len(redis_chat) >= 2 and redis_chat[-2].get("content") != user_input):
                        redis_chat.append({"role": "user", "content": user_input})
                        redis_chat.append({"role": "assistant", "content": response_text})

                        if len(redis_chat) >= 20:
                            messages_to_summarize = redis_chat[:10]
                            redis_chat = redis_chat[10:]
                            threading.Thread(target=summarize_and_store_memory, args=(storage_key, messages_to_summarize)).start()
                            logger.info(f"Summarization Triggered for {storage_key}. Truncating Redis chat.")

                        save_chat_history(storage_key, redis_chat)
    except Exception as e:
        logger.error(f"Error logging chat to Redis after_request: {e}")

    return response


"""
Streaming SSE endpoint — two route aliases:
  /stream                  → direct local access
  /upyog-voice-bot/stream  → production via niautt EKS ingress
GET requests return a health check response for Kubernetes liveness probes.
"""
@chat_bp.route("/stream", methods=["GET", "POST"])
@chat_bp.route("/upyog-voice-bot/stream", methods=["GET", "POST"])
@chat_bp.route("/upyog-voice/stream", methods=["GET", "POST"])
def stream():
    if request.method == "GET":
        logger.info("[ENDPOINT /stream GET] Health check ping")
        return jsonify({"status": "ok", "message": "UPYOG Voice Bot Stream Endpoint"}), 200

    import services.rag_service as rag_service
    from services.rag_service import retrieve_document_stream
    from services.intent_service import detect_language_per_turn
    from services.user_service import extract_phone_from_session
    from storage.redis_manager import get_chat_history

    try:
        if not rag_service.resources_ready_event.is_set() or any(x is None for x in [rag_service.model, rag_service.data, rag_service.index]):
            rag_service.resources_ready_event.wait(timeout=10)
            if any(x is None for x in [rag_service.model, rag_service.data, rag_service.index]):
                rag_service.load_resources()
            if any(x is None for x in [rag_service.model, rag_service.data, rag_service.index]):
                return Response("data: {\"error\": \"Loading resources...\"}\n\n", mimetype='text/event-stream'), 503

        user_data = request.json or {}
        user_input = user_data.get("query", "")
        session_id = user_data.get("session_id", "default")
        phone_anchor = extract_phone_from_session(session_id)
        history_key = phone_anchor if (phone_anchor and phone_anchor != "default") else session_id
        history = get_chat_history(history_key) if history_key else []
        if not history and user_data.get("history"):
            raw_hist = user_data.get("history")
            if isinstance(raw_hist, list):
                history = [h for h in raw_hist if isinstance(h, dict) and "role" in h and "content" in h]

        user_language, detected_script = detect_language_per_turn(user_input)
        logger.info(f"[ENDPOINT /stream POST] Stream request: '{user_input}' -> lang={user_language} (script: {detected_script})")

        return Response(retrieve_document_stream(user_input, user_language, history, phone_anchor=history_key), mimetype='text/event-stream')

    except Exception as e:
        logger.error(f"[ENDPOINT /stream ERROR] Exception: {e}")
        return Response(f"data: {json.dumps({'type': 'error', 'error': str(e)})}\n\n", mimetype='text/event-stream')


"""
Stop endpoint — called when the user interrupts (barges in) while the bot is speaking.
"""
@chat_bp.route("/stop", methods=["POST"])
@chat_bp.route("/upyog-voice-bot/stop", methods=["POST"])
@chat_bp.route("/upyog-voice/stop", methods=["POST"])
def stop():
    """Stop endpoint - called when user barges in."""
    from services.voice_service import stop_generation
    stop_generation.set()
    logger.info("[ENDPOINT /stop POST] Stop signal set — interrupting generation thread")
    return jsonify({"status": "stopped"}), 200


def register_chat_routes(app):
    """Registers chat Blueprint with the Flask app."""
    app.register_blueprint(chat_bp)
