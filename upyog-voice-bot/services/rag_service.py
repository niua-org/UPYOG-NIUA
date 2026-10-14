import os
import re
import time
import json
import logging
import threading
import numpy as np
import pandas as pd
from functools import lru_cache
from sentence_transformers import SentenceTransformer
import faiss
from dotenv import load_dotenv
from prompts.system_prompt import (
    HARD_BLOCK_TOPICS,
    build_rag_system_prompt,
    build_rag_stream_system_prompt,
    build_query_rewriter_prompt,
)

try:
    from groq import Groq
except ImportError:
    Groq = None

from services.voice_service import (
    translate_text,
    text_to_speech,
    stop_generation
)

load_dotenv(override=True)

logger = logging.getLogger(__name__)

GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
GROQ_MODEL = os.environ.get("GROQ_MODEL", "openai/gpt-oss-20b")
groq_client = None

# Global resources for FAISS and embeddings
model = None
data = None
index = None
frs_data = None
frs_index = None
prompt_embeddings = None
is_loading = False
load_lock = threading.Lock()
resources_ready_event = threading.Event()

FAISS_THRESHOLD = 1.08
EMBEDDING_MODEL = 'all-mpnet-base-v2'


def load_resources():
    """Loads all required AI models and FAISS resources into memory on startup."""
    global model, data, index, prompt_embeddings, frs_data, frs_index, is_loading

    with load_lock:
        if resources_ready_event.is_set() and model is not None and index is not None:
            return

        is_loading = True
        try:
            base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

            # 1. Initialize SentenceTransformer model
            if model is None:
                logger.info(f"Loading SentenceTransformer model ({EMBEDDING_MODEL})...")
                model = SentenceTransformer(EMBEDDING_MODEL)
                logger.info("SentenceTransformer model loaded successfully.")

            # 2. Load FAQ data & FAISS index
            data_path = os.path.join(base_dir, 'data', 'UpyogFAQ.csv')
            faq_idx_path = os.path.join(base_dir, 'data', 'UpyogFAQ_index.faiss')
            if os.path.exists(data_path):
                data = pd.read_csv(data_path)
                logger.info(f"FAQ data loaded from {data_path} ({len(data)} rows)")

                if os.path.exists(faq_idx_path):
                    index = faiss.read_index(faq_idx_path)
                    logger.info(f"FAQ FAISS index loaded from {faq_idx_path} ({index.ntotal} vectors).")
                else:
                    logger.info("Generating FAQ embeddings from scratch...")
                    prompt_embeddings = model.encode(data['prompt'].tolist())
                    dimension = prompt_embeddings.shape[1]
                    index = faiss.IndexFlatL2(dimension)
                    index.add(prompt_embeddings.astype(np.float32))
                    faiss.write_index(index, faq_idx_path)
                    logger.info(f"FAQ FAISS index generated and saved to {faq_idx_path}.")

            # 3. Load FRS Knowledge Base
            frs_path = os.path.join(base_dir, 'data', 'frs_smart_faq.csv')
            frs_idx_path = os.path.join(base_dir, 'data', 'frs_smart_index.faiss')
            if os.path.exists(frs_path) and os.path.exists(frs_idx_path):
                frs_data = pd.read_csv(frs_path)
                frs_index = faiss.read_index(frs_idx_path)
                logger.info(f"FRS Knowledge Base loaded with {len(frs_data)} specifications.")
            else:
                logger.warning("FRS Knowledge Base NOT found.")

            resources_ready_event.set()
            logger.info("All RAG resources loaded successfully.")

        except Exception as e:
            logger.error(f"Error loading RAG resources: {e}")
            raise
        finally:
            is_loading = False


# Start loading resources immediately
threading.Thread(target=load_resources, daemon=True).start()

def is_hard_blocked(query: str) -> bool:
    q = query.lower()
    blocked = any(topic in q for topic in HARD_BLOCK_TOPICS)
    if blocked:
        matched = [t for t in HARD_BLOCK_TOPICS if t in q]
        logger.info(f"[DOMAIN CHECK] Query '{query}' matched hard-block topics: {matched}")
    else:
        logger.debug(f"[DOMAIN CHECK] Query '{query}' passed hard-block check.")
    return blocked


def is_in_domain(query: str) -> tuple:
    if is_hard_blocked(query):
        return False, "out_of_domain"
    return True, "ok"


def get_rejection_message(reason: str, lang: str) -> str:
    logger.info(f"[REJECTION] Generating rejection message for reason='{reason}', lang='{lang}'")
    if reason == "out_of_domain":
        if lang == 'hi':
            return (
                "मैं केवल UPYOG और NUDM (National Urban Digital Mission) से संबंधित नागरिक सेवाओं में सहायता कर सकता हूँ, "
                "जैसे कि संपत्ति कर (Property Tax), व्यापार लाइसेंस (Trade License), जल और सीवरेज (Water & Sewerage), "
                "जन्म व मृत्यु प्रमाण पत्र, शिकायत निवारण आदि।\n\n"
                "कृपया शहरी सेवाओं से संबंधित प्रश्न पूछें।"
            )
        return (
            "I can only assist with UPYOG and NUDM (National Urban Digital Mission) citizen services, "
            "such as Property Tax, Trade License, Water & Sewerage, Birth & Death certificates, "
            "Grievance Redressal, and other municipal services.\n\n"
            "Please ask a question related to government urban services."
        )
    return (
        "क्षमा करें, मैं इस प्रश्न का उत्तर देने में असमर्थ हूँ। कृपया UPYOG सेवाओं से संबंधित प्रश्न पूछें।"
        if lang == 'hi' else
        "I'm sorry, I cannot answer this query. Please ask a question related to UPYOG urban services."
    )


# ============== RAG & RESPONSE GENERATION ==============

def contains_urdu_script(text: str) -> bool:
    return bool(re.compile(r'[؀-ۿ]').search(text))


def contextualize_query_for_search(query: str, history: list) -> str:
    """
    Resolves pronouns and references (e.g. 'it', 'fees', 'iska process kya hai') 
    using conversation history into a standalone search query for FAISS.
    """
    global groq_client
    if not history or not query or len(query.strip()) < 2:
        return query

    q_lower = query.lower().strip()
    pronoun_indicators = [
        "it", "this", "that", "its", "these", "those", "they", "them",
        "iska", "iski", "iske", "isme", "usme", "uska", "uski", "unka",
        "ye", "yeh", "woh", "aise", "fees", "fee", "cost", "charge",
        "charges", "document", "documents", "process", "time", "duration",
        "renewal", "apply", "eligibility", "step", "steps", "kaise",
        "kitna", "kitne", "kya chahiye", "kya lagega", "aur", "and", "tell me more"
    ]
    words = q_lower.split()
    is_context_dependent = (
        len(words) <= 7 or
        any(re.search(rf'\b{re.escape(w)}\b', q_lower) for w in pronoun_indicators)
    )
    if not is_context_dependent:
        return query

    try:
        api_key = GROQ_API_KEY or os.environ.get("GROQ_API_KEY")
        if not groq_client and Groq and api_key:
            groq_client = Groq(api_key=api_key)

        if not groq_client:
            return query

        recent_history = history[-4:] if len(history) >= 4 else history
        hist_lines = []
        for turn in recent_history:
            if isinstance(turn, dict):
                role = "User" if turn.get("role") == "user" else "Assistant"
                content = str(turn.get("content", ""))[:200]
                hist_lines.append(f"{role}: {content}")
            elif isinstance(turn, (list, tuple)) and len(turn) == 2:
                hist_lines.append(f"User: {str(turn[0])[:200]}")
                hist_lines.append(f"Assistant: {str(turn[1])[:200]}")

        if not hist_lines:
            return query

        history_text = "\n".join(hist_lines)
        prompt = build_query_rewriter_prompt(query=query, history_text=history_text)

        resp = groq_client.chat.completions.create(
            model=GROQ_MODEL,
            messages=[{"role": "user", "content": prompt}],
            max_tokens=300,
            temperature=0.0
        )
        rewritten = resp.choices[0].message.content.strip().strip('"\'').strip()
        # Take the first line if the model generated any extra line breaks
        if "\n" in rewritten:
            rewritten = rewritten.split("\n")[0].strip().strip('"\'')
        if rewritten and len(rewritten) >= 3 and len(rewritten) < 150:
            logger.info(f"[RAG Rewrite] Contextualized query: '{query}' -> '{rewritten}'")
            return rewritten
    except Exception as e:
        logger.debug(f"[RAG Rewrite] Query contextualization exception (safely falling back): {e}")

    return query


def get_rag_response(query: str, history: list, lang: str, search_lang: str = None, session_id: str = "default") -> str:
    """
    LLM-first architecture: LLM understands human language, FAISS provides optional context.
    FAISS is NOT a hard gate - if no context found, LLM answers from general knowledge.
    """
    global groq_client

    if search_lang is None:
        search_lang = lang

    # Fetch persistent profile values from helper
    try:
        from services.user_service import extract_phone_from_session, get_user_profile_info
        phone_anchor = extract_phone_from_session(session_id)
        user_info = get_user_profile_info(phone_anchor) if phone_anchor != "default" else get_user_profile_info(session_id)
    except Exception:
        phone_anchor = "default"
        user_info = None

    history_key = phone_anchor if (phone_anchor and phone_anchor != "default") else session_id

    long_term_bookings_str = ""
    long_term_chat_str = ""
    qdrant_summary_str = ""
    if history_key and history_key != "default":
        try:
            from storage.redis_manager import get_chat_history
            redis_chat = get_chat_history(history_key)
            if redis_chat:
                long_term_chat_str = "\n\nUSER'S PAST CHAT HISTORY (LONG-TERM REDIS MEMORY):\n"
                for msg in redis_chat[-15:]:
                    role_label = "User" if msg.get("role") == "user" else "Assistant"
                    long_term_chat_str += f"{role_label}: {msg.get('content')}\n"
                    
            from storage.qdrant_manager import MemoryManager
            if model:
                query_emb = model.encode([query])[0].tolist()
                past_summaries = MemoryManager.search_long_term_memory(history_key, query_emb, limit=3)
                if past_summaries:
                    qdrant_summary_str = "\n\nUSER'S PAST CHAT HISTORY (SUMMARIES FROM QDRANT):\n"
                    for s in past_summaries:
                        qdrant_summary_str += f"- [{s.get('date_str')}] {s.get('content')}\n"
        except Exception as e:
            logger.error(f"Error loading chat history or summaries for RAG context: {e}")
    
    profile_details_str = "CITIZEN STATUS: Guest / Not Logged In."
    profile_name = None
    if user_info and phone_anchor != "default":
        profile_name = user_info.get("name") or user_info.get("userName") or "Citizen"
        profile_details_str = f"""ACTIVE CITIZEN PROFILE:
- Name: {profile_name}
- Mobile Number: {user_info.get("mobileNumber") or user_info.get("userName") or "N/A"}
- Email ID: {user_info.get("emailId") or "N/A"}
- Roles: {', '.join([r.get('name') for r in user_info.get('roles', [])]) if user_info.get('roles') else 'Citizen'}
- Tenant ID: {user_info.get("tenantId") or "pg"}"""

    if long_term_bookings_str:
        profile_details_str += long_term_bookings_str
    if qdrant_summary_str:
        profile_details_str += qdrant_summary_str
    if long_term_chat_str:
        profile_details_str += long_term_chat_str

    # Step 1: Try FAISS for supporting context
    context = ""
    try:
        # Contextualize follow-up queries using history
        contextual_query = contextualize_query_for_search(query, history)
        query_for_search = contextual_query
        if search_lang != 'en':
            translated = translate_text(contextual_query, search_lang, "en")
            if translated and len(translated.strip()) > 2:
                query_for_search = translated
                logger.info(f"[RAG FAISS] Translated query for vector search: '{query_for_search}'")

        relevant_chunks = []
        if model and index is not None:
            query_embedding = model.encode([query_for_search])
            distances, indices = index.search(query_embedding.astype(np.float32), k=5)

            for dist, idx in zip(distances[0], indices[0]):
                if idx >= 0 and dist < FAISS_THRESHOLD:
                    if data is not None and 'prompt' in data.columns and 'response' in data.columns:
                        relevant_chunks.append(f"Q: {data['prompt'].iloc[idx]}\nA: {data['response'].iloc[idx]}")

        # Also search FRS Knowledge Base if available
        if model and frs_index is not None:
            frs_dist, frs_indices = frs_index.search(model.encode([query_for_search]).astype(np.float32), k=5)
            for dist, idx in zip(frs_dist[0], frs_indices[0]):
                if idx >= 0 and dist < FAISS_THRESHOLD:
                    if frs_data is not None:
                        relevant_chunks.append(f"Q: {frs_data.iloc[idx]['question']}\nA: {frs_data.iloc[idx]['answer']}")

        if relevant_chunks:
            context = "\n\n".join(relevant_chunks[:3])
            logger.info(f"[RAG FAISS] FAISS context found: {len(relevant_chunks)} relevant chunks")
        else:
            logger.info("[RAG FAISS] No FAISS context found - LLM will answer from general knowledge")

    except Exception as e:
        logger.error(f"[RAG FAISS] FAISS search error (non-fatal): {e}")
        context = ""

    # Step 2: Build language instruction
    if lang == 'hi':
        lang_rule = "CRITICAL LANGUAGE INSTRUCTION: The user is asking in Hindi. You MUST respond in pure Hindi language using Devanagari script ONLY (हिंदी लिपि). Do NOT use Roman script, English sentences, or Romanized Hinglish under any circumstances. Exception: keep UPYOG, NUDM, NOC, GIS, ULB, MoU as-is."
    else:
        lang_rule = "CRITICAL LANGUAGE INSTRUCTION: The user is asking in English. You MUST respond in pure standard English script and language ONLY. Do NOT use Romanized Hinglish, Hindi words, or Devanagari script under any circumstances."

    # Step 3: Build context section
    context_section = f"""KNOWLEDGE BASE CONTEXT (use as primary reference):
{context}

{profile_details_str}""" if context else f"""NO SPECIFIC KNOWLEDGE BASE CONTEXT FOUND.
Answer using your general knowledge about UPYOG, NUDM, and Urban Local Body (ULB) government services in India.
Keep the answer accurate, professional, and helpful.

{profile_details_str}"""

    # Step 4: Build history
    history_messages = []
    for turn in (history or []):
        if isinstance(turn, dict) and "role" in turn and "content" in turn:
            history_messages.append({"role": turn["role"], "content": turn["content"]})
        elif isinstance(turn, (list, tuple)) and len(turn) == 2:
            history_messages.append({"role": "user", "content": turn[0]})
            history_messages.append({"role": "assistant", "content": turn[1]})

    # Step 5: System prompt from prompts/system_prompt.py
    system = build_rag_system_prompt(lang=lang, lang_rule=lang_rule, context_section=context_section)

    # Step 6: Call Groq
    messages = [{"role": "system", "content": system}]
    messages.extend(history_messages)
    messages.append({"role": "user", "content": query})

    logger.info(f"[GROQ RAG] Calling Groq API with {len(messages)} messages (history turns: {len(history_messages)})")
    start_time = time.time()

    try:
        if not groq_client and Groq:
            groq_client = Groq(api_key=GROQ_API_KEY)

        if not groq_client:
            return "I'm sorry, AI services are currently unconfigured."

        response = groq_client.chat.completions.create(
            model=GROQ_MODEL,
            messages=messages,
            max_tokens=650,
            temperature=0.1
        )
        ans = response.choices[0].message.content.strip()
        elapsed = time.time() - start_time
        logger.info(f"[GROQ RAG] Received answer in {elapsed:.2f}s (len: {len(ans)} chars)")

        if lang == 'en' and any('ऀ' <= c <= 'ॿ' for c in ans):
            logger.info("[GROQ RAG] Output contained Devanagari for English query — translating to English")
            translated = translate_text(ans, "hi", "en")
            if translated and len(translated.strip()) > 0:
                ans = translated
        elif lang == 'hi' and not any('ऀ' <= c <= 'ॿ' for c in ans):
            logger.info("[GROQ RAG] Output contained Non-Devanagari for Hindi query — translating to Hindi")
            translated = translate_text(ans, "en", "hi")
            if translated and len(translated.strip()) > 0:
                ans = translated
        return ans

    except Exception as e:
        err_str = str(e).lower()
        logger.error(f"[GROQ RAG] Groq error: {e}", exc_info=True)
        if any(w in err_str for w in ["rate_limit", "429", "token", "tpm", "quota", "too many requests"]):
            return (
                "एआई सहायक की टोकन सीमा कुछ समय के लिए पूरी हो गई है। कृपया थोड़ी देर प्रतीक्षा करें और संक्षिप्त प्रश्न पूछें।"
                if lang == 'hi' else
                "The AI assistant has temporarily reached its message token limit. Please wait a moment and try again with a shorter question."
            )
        elif any(w in err_str for w in ["context_length", "maximum context"]):
            return (
                "यह बातचीत अधिकतम सीमा से अधिक लंबी हो गई है। कृपया एक नया प्रश्न पूछें।"
                if lang == 'hi' else
                "This conversation has exceeded the maximum length. Please ask a concise question or start a fresh query."
            )
        return (
            "क्षमा करें, सर्वर से संपर्क नहीं हो पा रहा है। कृपया थोड़ी देर बाद पुनः प्रयास करें।"
            if lang == 'hi' else
            "I'm sorry, I am currently unable to process your request. Please try again in a few moments."
        )


def retrieve_document(query: str, user_lang: str, history: list, session_id: str = "default") -> str:
    """Retrieves relevant documents using an LLM-first approach with FAISS as optional context."""
    stop_generation.clear()
    return get_rag_response(query, history, user_lang, search_lang=user_lang, session_id=session_id)


def retrieve_document_stream(query: str, user_lang: str, history: list, phone_anchor: str = "default"):
    """Streaming version of document retrieval that yields text chunks for SSE rendering."""
    stop_generation.clear()
    logger.info(f"[STREAMING] Starting SSE stream for query='{query}', lang='{user_lang}'")

    try:
        contextual_query = contextualize_query_for_search(query, history)
        query_for_search = translate_text(contextual_query, user_lang, "en") if user_lang in ["hi", "mr", "bn", "gu", "ta", "te", "kn", "ml"] else contextual_query
        if not query_for_search or len(query_for_search.strip()) < 3:
            query_for_search = contextual_query

        faq_context = []
        if index is not None and model is not None:
            faq_dist, faq_indices = index.search(model.encode([query_for_search]).astype(np.float32), 3)
            for d, idx in zip(faq_dist[0], faq_indices[0]):
                if idx != -1 and d < FAISS_THRESHOLD:
                    if data is not None and 'prompt' in data.columns and 'response' in data.columns:
                        faq_context.append({"q": data['prompt'].iloc[idx], "a": data['response'].iloc[idx]})

        frs_context = []
        if frs_index is not None and model is not None:
            frs_dist, frs_indices = frs_index.search(model.encode([query_for_search]).astype(np.float32), 5)
            for d, idx in zip(frs_dist[0], frs_indices[0]):
                if idx != -1 and d < FAISS_THRESHOLD:
                    if frs_data is not None:
                        frs_context.append({"module": frs_data.iloc[idx]['module'], "text": f"Q: {frs_data.iloc[idx]['question']} A: {frs_data.iloc[idx]['answer']}"})

        logger.info(f"[STREAMING] Context chunks matched: FAQ={len(faq_context)}, FRS={len(frs_context)}")

        if not Groq or not GROQ_API_KEY:
            logger.warning("[STREAMING] Groq SDK/Key not present — sending fallback response")
            response_text = faq_context[0]['a'] if faq_context else "I'm sorry, I'm having trouble thinking right now."
            yield f"data: {json.dumps({'type': 'text', 'text': response_text})}\n\n"
            return

        client = Groq(api_key=GROQ_API_KEY)

        if user_lang == "hi":
            lang_instruction = (
                "CRITICAL: YOUR OUTPUT MUST BE IN HINDI DEVANAGARI SCRIPT ONLY.\n"
                "DO NOT USE ENGLISH ALPHABETS TO WRITE HINDI WORDS (No Hinglish).\n"
                "Example: Use 'नमस्ते' NOT 'Namaste'. Use 'उपयोग' NOT 'Upyog'.\n"
            )
        else:
            lang_instruction = "You MUST respond in clear, simple English only."

        context_str = ""
        if faq_context:
            context_str += "FAQ Knowledge:\n" + "\n".join([f"Q: {c['q']} A: {c['a']}" for c in faq_context])
        if frs_context:
            context_str += "\nTechnical Specs:\n" + "\n".join([c['text'] for c in frs_context])

        qdrant_summary_str = ""
        if phone_anchor != "default":
            try:
                from storage.qdrant_manager import MemoryManager
                if model:
                    query_emb = model.encode([query_for_search])[0].tolist()
                    past_summaries = MemoryManager.search_long_term_memory(phone_anchor, query_emb, limit=3)
                    if past_summaries:
                        qdrant_summary_str = "\nPAST CHAT SUMMARIES FROM QDRANT:\n" + "\n".join([f"- [{s.get('date_str')}] {s.get('content')}" for s in past_summaries])
            except Exception as e:
                logger.error(f"Error fetching Qdrant summaries in stream: {e}")

        system_instr = build_rag_stream_system_prompt(
            user_lang=user_lang,
            lang_instruction=lang_instruction,
            context_str=context_str,
            qdrant_summary_str=qdrant_summary_str
        )

        messages = [
            {"role": "system", "content": system_instr},
            *history[-10:],
            {"role": "user", "content": query}
        ]

        try:
            response = client.chat.completions.create(
                model=GROQ_MODEL,
                messages=messages,
                temperature=0.1,
                max_tokens=500,
                stream=True
            )

            full_response = ""
            for chunk in response:
                if stop_generation.is_set():
                    logger.info("Stream interrupted by stop signal")
                    break

                if chunk.choices and chunk.choices[0].delta.content:
                    content = chunk.choices[0].delta.content
                    full_response += content
                    yield f"data: {json.dumps({'type': 'text', 'text': content})}\n\n"

            # Generate TTS after full response
            if not stop_generation.is_set() and full_response:
                audio_output = text_to_speech(full_response, user_lang)
                if audio_output:
                    yield f"data: {json.dumps({'type': 'audio', 'audio': audio_output})}\n\n"

        except Exception as e:
            err_str = str(e).lower()
            logger.error(f"Streaming error: {e}", exc_info=True)
            if any(w in err_str for w in ["rate_limit", "429", "token", "tpm", "quota"]):
                friendly_err = (
                    "एआई सेवा की टोकन सीमा पूरी हो गई है। कृपया थोड़ी देर प्रतीक्षा करके संक्षिप्त प्रश्न पूछें।"
                    if user_lang == "hi" else
                    "The AI token limit has been reached. Please wait a moment and try again with a shorter message."
                )
            else:
                friendly_err = (
                    "सर्वर समस्या के कारण प्रतिक्रिया पूरी नहीं हो सकी। कृपया पुनः प्रयास करें।"
                    if user_lang == "hi" else
                    "Unable to complete the response due to a temporary server issue. Please try again."
                )
            yield f"data: {json.dumps({'type': 'text', 'text': friendly_err})}\n\n"

    except Exception as e:
        logger.error(f"Error in retrieve_document_stream: {e}", exc_info=True)
        fallback = (
            "क्षमा करें, इस समय संपर्क स्थापित नहीं हो सका। कृपया पुनः प्रयास करें।"
            if user_lang == "hi" else
            "Sorry, unable to establish connection at this time. Please try again."
        )
        yield f"data: {json.dumps({'type': 'text', 'text': fallback})}\n\n"
