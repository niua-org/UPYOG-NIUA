"""
services/chat_handlers.py — Modular Handler Pipeline for Chat Requests
======================================================================
Decomposes chat execution into dedicated, maintainable handler functions:
1. resolve_auth_and_profile — Auto-detects & authenticates user credentials
2. handle_login_intent — Manages login flow instructions & login claim confirmations
3. handle_pending_multi_draft — Resolves multi-draft selection menus
4. detect_special_intents — Pre-classifies draft, profile, and conversation control intents
5. resolve_active_and_target_plugin — Determines active workflow state and handles service switching
6. handle_workflow_interruption_or_faq — Handles mid-workflow FAQ interruptions with draft auto-save
7. handle_draft_management — Handles save, resume, delete, and continue draft actions
8. execute_workflow_plugin — Dispatches message to LangGraph plugin with language translation
9. execute_rag_fallback — Standard knowledge base RAG retrieval & domain validation
10. build_chat_error_response — User-friendly error message builder
"""

import os
import re
import json
import time
import logging
import yaml
from typing import Dict, Any, Optional, Tuple, List
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
GROQ_MODEL = os.environ.get("GROQ_MODEL", "openai/gpt-oss-20b")


def _load_system_messages() -> dict:
    """Loads system message responses and configurations from config.yml.

    Lookup Order:
        1. Project root configuration: `<project_root>/config.yml`
        2. Workflow directory fallback: `<project_root>/workflow/config.yml`
        3. Hardcoded empty dictionary `{}` if neither exists or parsing fails.

    Returns:
        dict: The 'system_messages' mapping containing greeting, blocked, and login responses.
    """
    try:
        from services.intent_service import _load_system_config
        cfg = _load_system_config()
        if isinstance(cfg, dict):
            return cfg.get("system_messages", {})
    except Exception as e:
        logger.warning(f"Could not load system_messages via _load_system_config: {e}")
    return {}

_SYSTEM_MESSAGES: dict = _load_system_messages()

from storage.qdrant_manager import MemoryManager
from services.voice_service import text_to_speech, translate_text

# ====================================================================
# CONFIG DEFAULTS & WORKFLOW METADATA REGISTRY
# ====================================================================

DEFAULT_WORKFLOW_INTENTS = [
    "grievance_candidate", "grievance_status_candidate", "grievance_confirm",
    "adv_candidate", "adv_status_candidate", "booking_candidate", "adv_confirm", "booking_confirm"
]

DEFAULT_QUESTION_STARTERS = [
    "what", "how", "why", "when", "where", "who", "which", "whose", "whom",
    "can i", "could you", "tell me", "explain", "meaning",
    "kya", "kaise", "kab", "kahan", "kyun", "kis", "batao", "bataiye"
]

DEFAULT_GOODBYE_KEYWORDS = [
    "bye", "goodbye", "exit", "quit", "tata", "alvida", "see you", "done for now", "band karo"
]

WORKFLOW_METADATA = {
    "adv_booking": {
        "draft_key": "draft_booking",
        "display_name": "Advertisement Booking",
        "display_name_hi": "विज्ञापन बुकिंग",
    },
    "grievance": {
        "draft_key": "draft_grievance",
        "display_name": "Grievance",
        "display_name_hi": "शिकायत",
    },
}


def get_workflow_draft_key(wf_name: str) -> Optional[str]:
    """Returns the state dictionary key corresponding to a workflow plugin."""
    meta = WORKFLOW_METADATA.get(wf_name)
    if meta:
        return meta["draft_key"]
    return None


def get_workflow_fields(wf_name: str) -> List[str]:
    """Returns all primary form fields for the specified workflow plugin."""
    if wf_name == "adv_booking":
        try:
            from workflow.adv_booking import ALL_FIELDS
            return list(ALL_FIELDS)
        except Exception:
            return ["addType", "location", "faceArea", "start_date", "end_date", "nightLight"]
    elif wf_name == "grievance":
        try:
            from workflow.grievance import ALL_FIELDS
            return list(ALL_FIELDS)
        except Exception:
            return ["category", "sub_category", "description", "locality"]
    return []


def get_workflow_reset_draft(wf_name: str, cancelled: bool = False) -> dict:
    """Constructs a clean, completely reset draft dictionary for a workflow plugin."""
    fields = get_workflow_fields(wf_name)
    reset_draft = {f: None for f in fields}
    if wf_name == "adv_booking":
        reset_draft["selected_slots"] = None
        reset_draft["applicant_details"] = None
        reset_draft["_awaiting_confirm"] = None
    elif wf_name == "grievance":
        reset_draft["category_code"] = None
        reset_draft["locality_code"] = None
        reset_draft["_awaiting_confirm"] = None
        reset_draft["_confirmed"] = None
    if cancelled:
        reset_draft["_cancelled"] = True
    return reset_draft


def is_draft_actively_in_progress(wf_name: str, state_values: dict) -> bool:
    """
    Checks if a workflow state represents an unsubmitted, actively in-progress draft.
    Returns False if the draft is cancelled, already submitted/empty, or absent.
    """
    if not isinstance(state_values, dict):
        return False
    draft_key = get_workflow_draft_key(wf_name)
    if not draft_key:
        return False
    draft = state_values.get(draft_key)
    if not isinstance(draft, dict) or draft.get("_cancelled"):
        return False
    has_fields = any(v for k, v in draft.items() if not k.startswith("_") and v is not None and str(v).strip() != "")
    has_options = bool(draft.get("_category_options") or draft.get("_sub_options") or draft.get("_locality_options") or draft.get("_slot_options"))
    messages = state_values.get("messages", [])
    return bool((has_fields or has_options) and messages)


def detect_active_workflow(phone: str, workflows: dict) -> Optional[str]:
    """Inspects available workflows and returns the active workflow name, if any."""
    config = {"configurable": {"thread_id": phone}}
    active_candidates = []
    for wf_name, graph in workflows.items():
        try:
            st = graph.get_state(config)
            if st and st.values and is_draft_actively_in_progress(wf_name, st.values):
                timestamp = getattr(st, "created_at", "") or ""
                msg_count = len(st.values.get("messages", []))
                active_candidates.append((wf_name, timestamp, msg_count))
        except Exception as e:
            logger.debug(f"[detect_active_workflow] Error inspecting {wf_name}: {e}")
    if active_candidates:
        active_candidates.sort(key=lambda x: (x[1], x[2]), reverse=True)
        return active_candidates[0][0]
    return None


# ====================================================================
# 1. PENDING INTERRUPTION & MULTI-DRAFT REDIS HELPERS
# ====================================================================

def set_pending_interruption(phone: str, data: dict):
    try:
        from storage.redis_manager import r_client
        r_client.set(f"pending_interruption:{phone}", json.dumps(data), ex=600)
    except Exception as e:
        logger.error(f"[Interruption] Redis set error: {e}")


def get_pending_interruption(phone: str) -> dict:
    try:
        from storage.redis_manager import r_client
        raw = r_client.get(f"pending_interruption:{phone}")
        if raw:
            return json.loads(raw)
    except Exception as e:
        logger.error(f"[Interruption] Redis get error: {e}")
    return {}


def clear_pending_interruption(phone: str):
    try:
        from storage.redis_manager import r_client
        r_client.delete(f"pending_interruption:{phone}")
    except Exception as e:
        logger.error(f"[Interruption] Redis delete error: {e}")


def set_multi_draft_pending(phone: str, drafts: list):
    try:
        from storage.redis_manager import r_client
        r_client.set(f"pending_multi_draft:{phone}", json.dumps({"status": "awaiting_multi_draft_choice", "drafts": drafts}), ex=600)
    except Exception as e:
        logger.warning(f"[MultiDraft] Redis set failed: {e}")


def get_multi_draft_pending(phone: str):
    try:
        from storage.redis_manager import r_client
        raw = r_client.get(f"pending_multi_draft:{phone}")
        if raw:
            return json.loads(raw)
    except Exception as e:
        logger.warning(f"[MultiDraft] Redis get failed: {e}")
    return None


def clear_multi_draft_pending(phone: str):
    try:
        from storage.redis_manager import r_client
        r_client.delete(f"pending_multi_draft:{phone}")
    except Exception:
        pass


def format_draft_summary(draft_data: dict, wf_name: str) -> str:
    title = wf_name.replace('_', ' ').title()
    summary = f"**{title} Draft**\n\n"
    for k, v in draft_data.items():
        if not k.startswith("_") and v is not None:
            clean_k = k.replace('_', ' ').title()
            summary += f"• **{clean_k}**: {v}\n"
    return summary


# ====================================================================
# 2. AUTHENTICATION & PROFILE RESOLUTION
# ====================================================================

def resolve_auth_and_profile(session_id: str, request_info: dict, user_data: dict) -> Tuple[str, Optional[str], Optional[dict], bool]:
    """
    Dynamically auto-detects and validates citizen identity and tokens.
    Returns: (phone_anchor, token, cached_info, is_authenticated)
    """
    from services.user_service import extract_phone_from_session, save_user_profile_info, get_user_profile_info
    from clients.upyog_client import verify_user_auth

    phone_anchor = extract_phone_from_session(session_id)
    token = None
    cached_info = None
    is_authenticated = False

    req_user_info = request_info.get("userInfo", {}) if isinstance(request_info, dict) else {}
    req_auth_token = request_info.get("authToken") if isinstance(request_info, dict) else None
    req_auth_token = req_auth_token or user_data.get("auth_token")

    # 1. First check if parent portal (e.g. NIUATT) passed RequestInfo with token & userInfo
    if req_user_info and req_auth_token and len(str(req_auth_token)) > 15:
        req_mobile = req_user_info.get("mobileNumber") or req_user_info.get("userName")
        clean_mobile = re.sub(r'\D', '', str(req_mobile or ''))[-10:]
        if clean_mobile and len(clean_mobile) == 10:
            phone_anchor = clean_mobile
            token = req_auth_token
            req_user_info["_auth_token"] = req_auth_token
            req_user_info["_verified_at"] = time.time()
            save_user_profile_info(phone_anchor, req_user_info)
            cached_info = req_user_info
            is_authenticated = True
            logger.info(f"[Auth] Auto-authenticated from RequestInfo for mobile={phone_anchor}")

    # 2. Check user-service Redis token store directly (access_token:<token>)
    if not is_authenticated:
        token = token or req_auth_token or user_data.get("auth_token") or (request_info.get("authToken") if isinstance(request_info, dict) else None)
        if token and len(str(token)) > 15:
            from storage.redis_manager import get_user_from_redis_token
            redis_user = get_user_from_redis_token(str(token))
            if redis_user:
                mobile = redis_user.get("mobileNumber") or redis_user.get("userName")
                clean_mobile = re.sub(r'\D', '', str(mobile or ''))[-10:]
                if clean_mobile and len(clean_mobile) == 10:
                    phone_anchor = clean_mobile
                    save_user_profile_info(phone_anchor, redis_user)
                    cached_info = redis_user
                    is_authenticated = True
                    logger.info(f"[Auth] Auto-authenticated from backbone Redis token store for mobile={phone_anchor}")

    # 3. Check session phone_anchor cache if not already authenticated
    if not is_authenticated and phone_anchor != "default":
        token = user_data.get("auth_token") or (request_info.get("authToken") if isinstance(request_info, dict) else None)
        if token and len(token) > 15:
            cached_info = get_user_profile_info(phone_anchor)
            cached_token = cached_info.get("_auth_token") if cached_info else None
            verified_at = cached_info.get("_verified_at", 0) if cached_info else 0

            # Trust cache if same token and verified in the last 10 minutes (600s)
            if cached_token == token and (time.time() - verified_at) < 600:
                is_authenticated = True
                logger.info(f"[Auth] Token for {phone_anchor} verified from cache (last check: {int(time.time() - verified_at)}s ago)")
            else:
                logger.info(f"[Auth] Token cache miss/expired for {phone_anchor}. Validating with UPYOG...")
                is_valid, fresh_user_info = verify_user_auth(token, phone_anchor)
                if is_valid:
                    fresh_user_info["_auth_token"] = token
                    fresh_user_info["_verified_at"] = time.time()
                    save_user_profile_info(phone_anchor, fresh_user_info)
                    cached_info = fresh_user_info
                    is_authenticated = True
                else:
                    from storage.redis_manager import r_client
                    r_client.delete(f"user_profile_info:{phone_anchor}")
                    cached_info = None
                    is_authenticated = False
                    logger.warning(f"[Auth] Token verification failed for {phone_anchor}, operating in guest mode")

    # 4. Fallback: query UPYOG /user/_search using the token
    if not is_authenticated:
        token = user_data.get("auth_token") or (request_info.get("authToken") if isinstance(request_info, dict) else None)
        if token and len(str(token)) > 15:
            is_valid, fresh_user_info = verify_user_auth(token, "default")
            if is_valid and fresh_user_info:
                mobile = fresh_user_info.get("mobileNumber") or fresh_user_info.get("userName")
                clean_mobile = re.sub(r'\D', '', str(mobile or ''))[-10:]
                if clean_mobile and len(clean_mobile) == 10:
                    phone_anchor = clean_mobile
                    fresh_user_info["_auth_token"] = token
                    fresh_user_info["_verified_at"] = time.time()
                    save_user_profile_info(phone_anchor, fresh_user_info)
                    cached_info = fresh_user_info
                    is_authenticated = True
                    logger.info(f"[Auth] Verified token with UPYOG and authenticated mobile={phone_anchor}")

    return phone_anchor, token, cached_info, is_authenticated


# ====================================================================
# 3. SPECIAL INTENT DETECTOR & LOGIN GUIDANCE
# ====================================================================

def handle_login_intent(
    user_input: str,
    user_language: str,
    is_authenticated: bool,
    cached_info: Optional[dict],
    detected_intent: Optional[str] = None
) -> Tuple[bool, Optional[dict]]:
    """Handles login guidance and login claim queries using config messages and detected intent.

    Args:
        user_input (str): The raw text message entered by the citizen.
        user_language (str): Target language code ('en' or 'hi').
        is_authenticated (bool): Whether citizen currently holds an authenticated session.
        cached_info (dict | None): User profile info (e.g. name, phone number) if logged in.
        detected_intent (str | None, optional): Explicit intent passed from unified classifier
            or fast-path matcher ('login_query' or 'login_claim'). If None, keyword matching is performed.

    Returns:
        Tuple[bool, Optional[dict]]:
            - bool: True if handled as a login action/query, False otherwise.
            - dict | None: Response payload dict ({response, lang, mode, audio}) if handled, else None.
    """
    from services.voice_service import text_to_speech

    sys_login = _SYSTEM_MESSAGES.get("login", {})

    # Fallback keyword match if detected_intent is not yet set
    if not detected_intent:
        ui_clean = user_input.lower().strip()
        login_kws = sys_login.get("keywords_query", ["how to login", "how do i login", "login kaise kare"])
        login_claim_kws = sys_login.get("keywords_claim", ["login done", "logged in", "login ho gaya"])
        if any(kw in ui_clean for kw in login_kws):
            detected_intent = "login_query"
        elif any(ui_clean == kw or ui_clean.startswith(kw) for kw in login_claim_kws):
            detected_intent = "login_claim"

    if detected_intent == "login_query":
        if user_language == "hi":
            login_msg = sys_login.get(
                "guidance_hi",
                "बाईं ओर के साइडबार (Left Sidebar) में नीचे जाएं और **Login** विकल्प पर क्लिक करें। "
                "अपना पंजीकृत मोबाइल नंबर भरें और फिर प्राप्त OTP दर्ज करें। "
                "लॉगिन करने के बाद, आप बुकिंग बना सकते हैं, शिकायत दर्ज कर सकते हैं और अपने आवेदन की स्थिति देख सकते हैं।"
            )
        else:
            login_msg = sys_login.get(
                "guidance_en",
                "In the left sidebar, scroll down and click on **Login**. "
                "Enter your registered mobile number and then enter the OTP received on your phone. "
                "Once logged in, you will be able to create bookings, register complaints, and check your application status."
            )
        audio_output = text_to_speech(login_msg, user_language)
        return True, {"response": login_msg, "lang": user_language, "mode": "faq", "audio": audio_output}

    if detected_intent == "login_claim":
        if is_authenticated:
            user_name = (cached_info.get("name") if cached_info else None) or "Citizen"
            if user_language == "hi":
                resp_template = sys_login.get(
                    "verified_hi",
                    "बहुत बढ़िया! आपकी पहचान सत्यापित हो गई है ({name})। अब आप विज्ञापन बुकिंग कर सकते हैं या शिकायत दर्ज कर सकते हैं। आप क्या करना चाहते हैं?"
                )
            else:
                resp_template = sys_login.get(
                    "verified_en",
                    "Great! Your login session is verified ({name}). You can now proceed to book an advertisement or register a complaint. How would you like to proceed?"
                )
            resp_msg = resp_template.replace("{name}", str(user_name))
        else:
            if user_language == "hi":
                resp_msg = sys_login.get(
                    "unverified_hi",
                    "मुझे अभी आपका सक्रिय लॉगिन सत्र नहीं मिला है। कृपया बाईं ओर के साइडबार में नीचे **Login** विकल्प पर क्लिक करके अपने मोबाइल नंबर और OTP से लॉगिन पूरा करें।"
                )
            else:
                resp_msg = sys_login.get(
                    "unverified_en",
                    "I do not detect an active logged-in session yet. Please complete the login by clicking **Login** in the left sidebar and entering your mobile number and OTP."
                )
        audio_output = text_to_speech(resp_msg, user_language)
        return True, {"response": resp_msg, "lang": user_language, "mode": "faq", "audio": audio_output}

    return False, None


def detect_special_intents(user_input: str) -> Optional[dict]:
    """Bypasses LLM classification for structured UI JSON payloads."""
    ui_lower = user_input.strip().lower()
    if (ui_lower.startswith("[") and ui_lower.endswith("]")) or (ui_lower.startswith("{") and ui_lower.endswith("}")):
        return {"intent": "none", "service": "None", "emotion": "neutral"}
    return None


# ====================================================================
# 4. PENDING MULTI-DRAFT INTERCEPTION
# ====================================================================

def handle_pending_multi_draft(user_input: str, user_language: str, phone: str, session_id: str, workflows: dict) -> Tuple[bool, Optional[dict]]:
    """Resolves pending multi-draft selection menu if active."""
    pending_multi = get_multi_draft_pending(phone)
    if not (pending_multi and pending_multi.get("status") == "awaiting_multi_draft_choice"):
        return False, None

    from draft_switcher import MultiDraftSwitcher
    from services.voice_service import text_to_speech
    from services.intent_service import process_user_message

    drafts = pending_multi.get("drafts", [])
    ui_check = user_input.strip().lower()
    digits = re.findall(r'\d+', ui_check)
    selected_idx = int(digits[0]) - 1 if digits else -1
    delete_keywords = ["delete", "cancel", "clear", "discard", "remove", "erase", "hatao", "hata", "mitado"]
    is_delete_choice = selected_idx == len(drafts) + 1 or any(w in ui_check for w in delete_keywords)

    if is_delete_choice:
        clear_multi_draft_pending(phone)
        return False, None

    selected_draft = MultiDraftSwitcher.resolve_citizen_selection(user_input, drafts)
    clear_multi_draft_pending(phone)

    if selected_draft:
        target_wf = selected_draft.get("plugin_name")
        draft_data = selected_draft.get("draft_data", {})

        if target_wf in workflows and draft_data:
            draft_key = "draft_booking" if target_wf == "adv_booking" else "draft_grievance"
            config_update = {"configurable": {"thread_id": phone if (phone and phone != "default") else session_id}}
            workflows[target_wf].update_state(config_update, {draft_key: draft_data})

        agent_res = process_user_message("continue", phone, session_id, target_workflow=target_wf)
        audio = text_to_speech(agent_res.get("response", ""), user_language)
        return True, {
            "response": agent_res.get("response", ""),
            "messages": agent_res.get("messages_list", []),
            "lang": user_language,
            "mode": "agent_active",
            "audio": audio,
            "input_type": agent_res.get("input_type", "text"),
            "options": agent_res.get("options", []),
            "show_button": agent_res.get("show_button")
        }

    return False, None


# ====================================================================
# 5. ACTIVE PLUGIN & TARGET ROUTING RESOLUTION
# ====================================================================

def _has_valid_tracking_id(text: str, id_prefixes: list) -> bool:
    """Checks whether the text contains a genuine tracking/application ID (e.g. ADV-1234, PGR-5678, ADV1234),
    avoiding accidental substring matches like 'adv' in 'advertisement'.
    """
    t_lower = text.lower()
    for p in id_prefixes:
        clean_p = p.rstrip("-").strip().lower()
        if not clean_p:
            continue
        if re.search(rf'\b{re.escape(clean_p)}[-_\s]?\d+', t_lower):
            return True
        if p.endswith("-") and re.search(rf'\b{re.escape(p.lower())}[a-z0-9_-]+', t_lower):
            return True
    return False


def is_informational_question(
    user_input: str,
    intent_data: Optional[dict] = None,
    system_config: Optional[dict] = None
) -> bool:
    """Checks whether the user input is an informational question or FAQ query.

    Matching rules:
    1. Returns True if classifier intent is 'faq', 'is_question' is True, or '?' is present.
    2. Otherwise checks question starters loaded from system_config (config.yml) under
       `system_messages.faq_question.question_starters` (fallback: `DEFAULT_QUESTION_STARTERS`).
    3. Input matches if it is identical to a starter or starts with `<starter> ` (word boundary).
    """
    if not user_input:
        return False
    intent = (intent_data or {}).get("intent", "")
    is_question = bool((intent_data or {}).get("is_question", False))
    if intent == "faq" or is_question or "?" in user_input:
        return True

    sys_msgs = (system_config or {}).get("system_messages") or _SYSTEM_MESSAGES or {}
    starters = (sys_msgs.get("faq_question") or {}).get("question_starters") or DEFAULT_QUESTION_STARTERS

    clean_in = user_input.strip().lower()
    return any(clean_in == q.lower() or clean_in.startswith(f"{q.lower()} ") for q in starters)


def resolve_active_and_target_plugin(
    user_input: str,
    intent: str,
    phone: str,
    session_id: str,
    workflows: dict,
    services_registry: list,
    config: dict,
    is_question: bool = False
) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    """
    Identifies active module and matches incoming message to target plugin.
    Handles service switching with draft auto-save.
    Returns: (active_plugin, plugin_intent, target_wf)
    """
    active_plugin = detect_active_workflow(phone, workflows)

    plugin_intent = None
    ui_lower = user_input.lower().strip()

    is_query_question = is_informational_question(user_input, {"intent": intent, "is_question": is_question})

    # If the user query is an informational question or FAQ, do NOT set any workflow intent
    if not is_query_question and intent != "faq":
        for srv in services_registry:
            s_key = srv.get("key")
            if s_key not in workflows:
                continue
            keywords = [k.lower() for k in srv.get("keywords", [])]
            id_prefixes = srv.get("id_prefixes", [])

            has_id = _has_valid_tracking_id(ui_lower, id_prefixes)
            has_kw = any(re.search(rf'\b{re.escape(w)}\b', ui_lower) for w in keywords)

            if has_id or has_kw:
                plugin_intent = s_key
                break

        if not plugin_intent and intent != "profile":
            if intent in ["grievance_candidate", "grievance_status_candidate"]:
                plugin_intent = "grievance"
            elif intent in ["adv_candidate", "adv_status_candidate", "booking_candidate"]:
                plugin_intent = "adv_booking"
            elif intent in ["adv_confirm", "booking_confirm", "grievance_confirm"]:
                plugin_intent = active_plugin or ("adv_booking" if "adv" in intent or "booking" in intent else "grievance")

    is_generic_response = ui_lower in ["yes", "no", "yeah", "yup", "ya", "ok", "okay", "sure", "confirm", "cancel", "haan", "nahi", "nahin", "1", "2", "option 1", "option 2"]
    if plugin_intent and active_plugin and plugin_intent != active_plugin and not is_generic_response:
        logger.info(f"[Router] User switched service: {active_plugin} -> {plugin_intent}")
        if active_plugin in workflows:
            prev_state = workflows[active_plugin].get_state(config)
            if prev_state and prev_state.values:
                prev_draft_key = get_workflow_draft_key(active_plugin)
                if prev_draft_key:
                    prev_draft = prev_state.values.get(prev_draft_key) or {}
                    if prev_draft and any(v for k, v in prev_draft.items() if not k.startswith("_") and v is not None and str(v).strip() != ""):
                        try:
                            MemoryManager.save_draft_state(phone, active_plugin, prev_draft)
                            logger.info(f"[AutoSave] Saved previous workflow draft for {phone}/{active_plugin}")
                        except Exception as e:
                            logger.warning(f"[AutoSave] Qdrant save draft failed: {e}")

                    empty_draft = get_workflow_reset_draft(active_plugin)
                    workflows[active_plugin].update_state(config, {prev_draft_key: empty_draft, "missing_fields": [], "messages": []})
        active_plugin = plugin_intent
    elif active_plugin and is_generic_response:
        plugin_intent = active_plugin

    cancel_intents = {"adv_cancel", "booking_cancel", "grievance_cancel"}
    if is_query_question or intent == "faq" or intent in cancel_intents:
        target_wf = None
    else:
        target_wf = plugin_intent or active_plugin
    return active_plugin, plugin_intent, target_wf


# ====================================================================
# 6. WORKFLOW INTERRUPTION & MID-FLOW FAQ
# ====================================================================

def handle_workflow_interruption_or_faq(
    user_input: str,
    user_language: str,
    history: list,
    session_id: str,
    phone: str,
    active_plugin: Optional[str],
    workflows: dict,
    config: dict,
    is_ui_payload: bool,
    intent_data: Optional[dict] = None
) -> Tuple[bool, Optional[dict]]:
    """Detects mid-workflow questions via LLM intent classification, auto-saves active draft, and responds with FAQ.

    Args:
        user_input (str): The current user query or response.
        user_language (str): Language code of the user ('en', 'hi').
        history (list): Recent conversation turns.
        session_id (str): Chat session identifier.
        phone (str): User phone number.
        active_plugin (str | None): Currently active LangGraph workflow plugin (e.g. 'grievance', 'adv_booking').
        workflows (dict): Registry of loaded workflow plugin graphs.
        config (dict): LangGraph runtime config with thread_id.
        is_ui_payload (bool): True if the input was generated from a UI button/selection rather than free text.
        intent_data (dict | None, optional): Unified intent classification payload from classify_intent,
            containing 'intent', 'is_question', 'classifier_failed', and 'reasoning'.

    Returns:
        Tuple[bool, Optional[dict]]:
            - bool: True if the input was handled as a mid-workflow question/FAQ, False otherwise.
            - dict | None: Response dictionary containing FAQ response and draft save note if interrupted.
    """
    if not active_plugin or active_plugin not in workflows or is_ui_payload:
        return False, None

    # 1. Option selection guard: check if user_input matches active workflow options or numbers
    is_option_selection = False
    clean_in = re.sub(r'[\s_]+', '', user_input.strip().lower())
    is_digit_choice = bool(re.match(r'^(?:option\s*)?\d+$', user_input.strip().lower()))
    generic_responses = {"yes", "no", "yeah", "yup", "ya", "ok", "okay", "sure", "confirm", "cancel", "haan", "nahi", "nahin", "1", "2", "option 1", "option 2"}

    if is_digit_choice or user_input.strip().lower() in generic_responses:
        is_option_selection = True
    else:
        state = workflows[active_plugin].get_state(config)
        if state and state.values:
            draft_key = get_workflow_draft_key(active_plugin)
            draft = (state.values.get(draft_key) or {}) if draft_key else {}

            if active_plugin == "grievance":
                from workflow.grievance import _pgr_categories, _pgr_localities
                pgr_cats = draft.get("_category_options") or _pgr_categories() or {}
                cats = list(pgr_cats.keys())
                pgr_locs = draft.get("_locality_options") or _pgr_localities() or []
                locs = [l.get("name", "") if isinstance(l, dict) else str(l) for l in pgr_locs]
            else:
                cats = list((draft.get("_category_options") or {}).keys())
                locs = [l.get("name", "") if isinstance(l, dict) else str(l) for l in (draft.get("_locality_options") or [])]

            subs = [s.get("name", "") if isinstance(s, dict) else str(s) for s in (draft.get("_sub_options") or [])]
            slots = [s.get("name", "") if isinstance(s, dict) else str(s) for s in (draft.get("_slot_options") or [])]

            all_opts = [re.sub(r'[\s_]+', '', str(o).lower()) for o in (cats + subs + locs + slots) if o]
            if clean_in in all_opts or any(clean_in == o or (o in clean_in and len(user_input.split()) <= 3) for o in all_opts):
                is_option_selection = True

    if is_option_selection:
        return False, None

    # 2. Never interrupt if classifier failed, was unconfigured, or reasoning indicates failure
    if (intent_data or {}).get("classifier_failed") or (intent_data or {}).get("reasoning") in ["classifier failed", "Groq client unconfigured"]:
        return False, None

    # 3. Determine if this message is an informational question or FAQ
    is_explicit_question = is_informational_question(user_input, intent_data)

    if is_explicit_question:
        from services.rag_service import retrieve_document

        draft_saved = False
        saved_plugin_name = None

        state = workflows[active_plugin].get_state(config)
        if state and state.values:
            draft_key = get_workflow_draft_key(active_plugin)
            if draft_key:
                draft = state.values.get(draft_key) or {}
                real_fields = {k: v for k, v in draft.items() if not k.startswith("_") and v is not None and str(v).strip() != ""}
                if real_fields:
                    try:
                        MemoryManager.save_draft_state(phone, active_plugin, draft)
                        draft_saved = True
                        saved_plugin_name = active_plugin
                    except Exception as e:
                        logger.warning(f"[handle_workflow_interruption] Qdrant save draft failed: {e}")

                empty_draft = get_workflow_reset_draft(active_plugin)
                workflows[active_plugin].update_state(config, {draft_key: empty_draft, "missing_fields": []})

        faq_ans = retrieve_document(user_input, user_language, history, session_id=session_id)

        draft_note = ""
        if draft_saved and saved_plugin_name:
            plugin_display = "Advertisement Booking" if saved_plugin_name == "adv_booking" else "Grievance"
            if user_language == 'hi':
                draft_note = f"\n\n*(नोट: आपका {plugin_display} ड्राफ्ट सुरक्षित सेव कर लिया गया है। इसे जारी रखने के लिए कभी भी 'Continue my application' कहें या ड्राफ्ट चुनें।)*"
            else:
                draft_note = f"\n\n*(Note: Your {plugin_display} application draft has been saved. You can continue it anytime by saying 'Continue my application' or selecting it from your drafts.)*"

        full_ans = f"{faq_ans}{draft_note}"
        audio = text_to_speech(full_ans, user_language)
        return True, {
            "response": full_ans,
            "lang": user_language,
            "mode": "faq",
            "audio": audio,
            "input_type": "text",
            "options": []
        }

    return False, None


# ====================================================================
# 7. DRAFT MANAGEMENT ACTIONS
# ====================================================================

def handle_draft_management(intent: str, user_input: str, user_language: str, phone: str, session_id: str, active_plugin: Optional[str], workflows: dict, config: dict, system_config: Optional[dict] = None) -> Tuple[bool, Optional[dict]]:
    """Handles explicit draft operations (continue, save, resume, delete, profile)."""
    from services.intent_service import process_user_message
    from draft_switcher import MultiDraftSwitcher, _SERVICES_MAP
    ui_lower = user_input.lower()

    if intent == "draft_continue_application":
        all_drafts = MemoryManager.get_all_draft_states(phone)
        if len(all_drafts) == 1:
            target_wf = all_drafts[0].get("plugin_name", "adv_booking")
            draft_data = all_drafts[0].get("draft_data", {})
            if target_wf in workflows and draft_data:
                draft_key = get_workflow_draft_key(target_wf)
                if draft_key:
                    config_update = {"configurable": {"thread_id": phone if (phone and phone != "default") else session_id}}
                    workflows[target_wf].update_state(config_update, {draft_key: draft_data})

            agent_res = process_user_message("continue", phone, session_id, target_workflow=target_wf, user_language=user_language)
            audio = text_to_speech(agent_res.get("response", ""), user_language)
            return True, {
                "response": agent_res.get("response", ""),
                "messages": agent_res.get("messages_list", []),
                "lang": user_language,
                "mode": "agent_active",
                "audio": audio,
                "input_type": agent_res.get("input_type", "text"),
                "options": agent_res.get("options", []),
                "min_date": agent_res.get("min_date"),
                "field": agent_res.get("field"),
                "show_button": agent_res.get("show_button")
            }
        elif len(all_drafts) > 1:
            switcher_res = MultiDraftSwitcher.inspect_and_render_switcher(phone, user_input=user_input, user_language=user_language)
            set_multi_draft_pending(phone, switcher_res["drafts"])
            msg = switcher_res["menu"]
            audio = text_to_speech(msg, user_language)
            return True, {
                "response": msg,
                "lang": user_language,
                "mode": "agent_active",
                "audio": audio,
                "input_type": "choice",
                "options": switcher_res.get("options") or [f"Option {i+1}" for i in range(switcher_res["count"])] + ["Start New Service Request"],
                "show_button": True
            }
        else:
            msg = (
                "Aapka koi saved draft nahi mila. Kya aap naya application start karna chahte hain?"
                if user_language == 'hi' else
                "I couldn't find any saved drafts. Would you like to start a new application?"
            )
            audio = text_to_speech(msg, user_language)
            return True, {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    if intent == "draft_save":
        plugin = active_plugin or "adv_booking"
        if plugin and plugin in workflows:
            config_check = {"configurable": {"thread_id": phone if (phone and phone != "default") else session_id}}
            state = workflows[plugin].get_state(config_check)
            if state and state.values:
                draft_key = get_workflow_draft_key(plugin)
                if draft_key:
                    draft = state.values.get(draft_key) or {}
                    if draft:
                        try:
                            MemoryManager.save_draft_state(phone, plugin, draft)
                        except Exception as e:
                            logger.warning(f"[handle_draft_management] Qdrant save draft failed: {e}")
        msg = "Your draft application has been saved successfully. You can resume it anytime by saying 'Continue my application'!"
        audio = text_to_speech(msg, user_language)
        return True, {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    if intent == "end_conversation":
        sys_msgs = (system_config or {}).get("system_messages") or _SYSTEM_MESSAGES or {}
        goodbye_kws = sys_msgs.get("goodbye_keywords") or DEFAULT_GOODBYE_KEYWORDS
        clean_in = user_input.strip().lower()
        is_short_msg = len(clean_in.split()) <= 4
        is_explicit_goodbye = is_short_msg and any(re.search(rf'\b{re.escape(w)}\b', clean_in) for w in goodbye_kws)

        # If citizen is inside an active workflow and did NOT say an explicit goodbye, do NOT abort the form
        if active_plugin and not is_explicit_goodbye:
            return False, None

        # Auto-save active draft before exiting if citizen says an explicit goodbye
        if active_plugin and active_plugin in workflows:
            state = workflows[active_plugin].get_state(config)
            if state and state.values:
                draft_key = get_workflow_draft_key(active_plugin)
                if draft_key:
                    draft = state.values.get(draft_key) or {}
                    if draft and any(v for k, v in draft.items() if not k.startswith("_") and v is not None and str(v).strip() != ""):
                        try:
                            MemoryManager.save_draft_state(phone, active_plugin, draft)
                        except Exception as e:
                            logger.warning(f"[handle_draft_management] Qdrant auto-save on goodbye failed: {e}")

        clear_pending_interruption(phone)
        msg = (
            "अलविदा! आपका दिन शुभ हो!"
            if user_language == "hi" else
            "Goodbye! Have a great day!"
        )
        audio = text_to_speech(msg, user_language)
        return True, {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    if intent == "draft_resume":
        switcher_res = MultiDraftSwitcher.inspect_and_render_switcher(phone, user_input=user_input, user_language=user_language)
        if switcher_res["has_drafts"]:
            msg = switcher_res["menu"]
            if switcher_res["count"] == 1:
                single_draft = switcher_res.get("single_draft") or switcher_res["drafts"][0]
                plugin = single_draft.get("plugin_name", "adv_booking")
                draft_data = single_draft.get("draft_data", {})
                summary = format_draft_summary(draft_data, plugin)
                msg = f"{summary}\n\nWhat would you like to do?"
                set_pending_interruption(phone, {"plugin": plugin, "status": "awaiting_resume"})
                audio = text_to_speech(msg, user_language)
                return True, {
                    "response": msg, "lang": user_language, "mode": "agent_active", "audio": audio,
                    "input_type": "choice", "options": ["Continue Application", "Cancel Draft"], "show_button": True
                }
            else:
                set_multi_draft_pending(phone, switcher_res["drafts"])
                audio = text_to_speech(msg, user_language)
                return True, {
                    "response": msg, "lang": user_language, "mode": "agent_active", "audio": audio,
                    "input_type": "choice",
                    "options": switcher_res.get("options") or [f"Option {i+1}" for i in range(switcher_res["count"])] + ["Start New Service Request"],
                    "show_button": True
                }
        else:
            msg = "I couldn't find any saved drafts for your account."
            audio = text_to_speech(msg, user_language)
            return True, {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    if intent in ["draft_cancel", "draft_cancel_application", "draft_delete"]:
        pending = get_pending_interruption(phone)
        clear_pending_interruption(phone)
        clear_multi_draft_pending(phone)

        from storage.redis_manager import clear_short_term_memory
        clear_short_term_memory(phone)

        target_plugins = []
        if "grievance" in ui_lower or "complaint" in ui_lower or "shikayat" in ui_lower:
            target_plugins = ["grievance"]
        elif "booking" in ui_lower or "advertisement" in ui_lower or "adv" in ui_lower or "ad" in ui_lower:
            target_plugins = ["adv_booking"]
        else:
            digits = re.findall(r'\d+', ui_lower)
            all_current = MemoryManager.get_all_draft_states(phone)
            if digits and all_current:
                num = int(digits[0]) - 1
                if 0 <= num < len(all_current):
                    target_plugins = [all_current[num].get("plugin_name")]
            elif pending and pending.get("plugin"):
                target_plugins = [pending["plugin"]]
            elif active_plugin and ("my drafts" not in ui_lower and "all" not in ui_lower and "drafts" not in ui_lower):
                target_plugins = [active_plugin]
            else:
                target_plugins = list(workflows.keys())

        is_delete_all = set(target_plugins) == set(workflows.keys()) or any(w in ui_lower for w in ["all", "drafts", "saare", "sab"])
        if is_delete_all:
            MemoryManager.delete_draft_state(phone)
            for wf in workflows.keys():
                config_update = {"configurable": {"thread_id": phone if (phone and phone != "default") else session_id}}
                draft_key = "draft_booking" if wf == "adv_booking" else "draft_grievance"
                empty_draft = {f: None for f in (["category", "sub_category", "description", "locality"] if wf == "grievance" else ["addType", "location", "faceArea", "start_date", "end_date", "nightLight"])}
                workflows[wf].update_state(config_update, {draft_key: empty_draft, "missing_fields": []})
                try:
                    process_user_message("[CANCEL_DRAFT]", phone, session_id, target_workflow=wf)
                except Exception as e:
                    logger.warning(f"[DraftDelete] Failed running [CANCEL_DRAFT] on {wf}: {e}")
            msg = (
                "Aapke sabhi saved drafts safaltapoorvak delete kar diye gaye hain."
                if user_language == 'hi' else
                "Your saved drafts have been deleted successfully."
            )
        else:
            for wf in target_plugins:
                MemoryManager.delete_draft_state(phone, wf)
                if wf in workflows:
                    config_update = {"configurable": {"thread_id": phone if (phone and phone != "default") else session_id}}
                    draft_key = "draft_booking" if wf == "adv_booking" else "draft_grievance"
                    empty_draft = {f: None for f in (["category", "sub_category", "description", "locality"] if wf == "grievance" else ["addType", "location", "faceArea", "start_date", "end_date", "nightLight"])}
                    workflows[wf].update_state(config_update, {draft_key: empty_draft, "missing_fields": []})
                    try:
                        process_user_message("[CANCEL_DRAFT]", phone, session_id, target_workflow=wf)
                    except Exception as e:
                        logger.warning(f"[DraftDelete] Failed running [CANCEL_DRAFT] on {wf}: {e}")

            remaining = MemoryManager.get_all_draft_states(phone)
            plugin_display = _SERVICES_MAP.get(target_plugins[0], {}).get("name", target_plugins[0].replace('_', ' ').title()) if target_plugins else "Draft"
            if user_language == 'hi':
                msg = f"Aapka **{plugin_display}** draft delete kar diya gaya hai." + (f" Aapke paas abhi {len(remaining)} saved draft(s) baaki hain." if remaining else "")
            else:
                msg = f"Your **{plugin_display}** draft has been deleted successfully." + (f" You have {len(remaining)} saved draft(s) remaining." if remaining else "")

        audio = text_to_speech(msg, user_language)
        return True, {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    return False, None


# ====================================================================
# 8. WORKFLOW EXECUTION & TRANSLATION
# ====================================================================

def execute_workflow_plugin(target_wf: str, user_input: str, user_language: str, phone: str, session_id: str, is_authenticated: bool) -> dict:
    """Executes target LangGraph plugin workflow with language checks."""
    from services.voice_service import text_to_speech, translate_text
    from services.intent_service import process_user_message

    if not is_authenticated:
        msg = (
            "शिकायत दर्ज करने या विज्ञापन बुकिंग के लिए, कृपया पहले लॉगिन करें। बाईं ओर के साइडबार में नीचे जाएं और **Login** विकल्प पर क्लिक करें, मोबाइल नंबर भरें और OTP दर्ज करें।"
            if user_language == 'hi' else
            "To file a complaint or create an advertisement booking, please log in first."
        )
        audio = text_to_speech(msg, user_language)
        return {
            "response": msg,
            "lang": user_language,
            "mode": "auth_required",
            "auth_required": True,
            "audio": audio
        }

    agent_res = process_user_message(user_input, phone, session_id, target_workflow=target_wf, user_language=user_language)
    resp_content = agent_res.get("response", "")

    if user_language == 'en' and any('ऀ' <= c <= 'ॿ' for c in resp_content):
        trans = translate_text(resp_content, "hi", "en")
        if trans and len(trans.strip()) > 0:
            resp_content = trans
    elif user_language == 'hi' and not any('ऀ' <= c <= 'ॿ' for c in resp_content):
        trans = translate_text(resp_content, "en", "hi")
        if trans and len(trans.strip()) > 0:
            resp_content = trans

    audio = text_to_speech(resp_content, user_language)
    return {
        "response": resp_content,
        "messages": agent_res.get("messages_list", []),
        "lang": user_language,
        "mode": "agent_active",
        "audio": audio,
        "input_type": agent_res.get("input_type", "text"),
        "options": agent_res.get("options", []),
        "min_date": agent_res.get("min_date"),
        "field": agent_res.get("field"),
        "show_button": agent_res.get("show_button"),
        "redirect_url": agent_res.get("redirect_url")
    }


# ====================================================================
# 9. RAG / FAQ RETRIEVAL FALLBACK
# ====================================================================

def execute_rag_fallback(user_input: str, user_language: str, detected_script: str, history: list, session_id: str) -> dict:
    """Executes domain checking, Faiss/Groq retrieval, and grievance offering checks."""
    from services.rag_service import is_in_domain, get_rejection_message, retrieve_document
    from services.voice_service import text_to_speech

    in_domain, reason = is_in_domain(user_input)
    if not in_domain and reason == "out_of_domain":
        message = get_rejection_message("out_of_domain", user_language)
        audio_output = text_to_speech(message, user_language)
        return {
            "response": message,
            "lang": user_language,
            "mode": "rejected",
            "reason": reason,
            "audio": audio_output
        }

    if not in_domain and reason == "too_short":
        message = get_rejection_message("too_short", user_language)
        audio_output = text_to_speech(message, user_language)
        return {
            "response": message,
            "lang": user_language,
            "mode": "clarify",
            "audio": audio_output
        }

    response_text = retrieve_document(user_input, user_language, history, session_id=session_id)

    if response_text and ("शिकायत" in response_text or "grievance" in response_text.lower() or "एक शिकायत" in response_text):
        audio_output = text_to_speech(response_text, user_language)
        return {
            "response": response_text,
            "lang": user_language,
            "mode": "grievance_offered",
            "audio": audio_output
        }

    response_text = re.sub(r'\bUpyog\b', 'UPYOG', response_text, flags=re.IGNORECASE)
    audio_output = text_to_speech(response_text, user_language)

    return {
        "response": response_text,
        "lang": user_language,
        "detected_script": detected_script,
        "mode": "faq",
        "audio": audio_output
    }


# ====================================================================
# 10. ERROR RESPONSE BUILDER
# ====================================================================

def build_chat_error_response(error: Exception, user_language: str = "en") -> dict:
    """Constructs localized, user-friendly fallback error messages."""
    from services.voice_service import text_to_speech

    err_str = str(error).lower()
    is_hi = (user_language == "hi")

    if any(w in err_str for w in ["rate_limit", "429", "token", "tpm", "quota", "too many requests"]):
        fallback_msg = (
            "एआई सहायक की टोकन सीमा कुछ समय के लिए पूरी हो गई है। कृपया थोड़ी देर प्रतीक्षा करें और संक्षिप्त प्रश्न पूछें।"
            if is_hi else
            "The AI assistant has temporarily reached its message token limit. Please wait a moment and try again with a shorter question."
        )
        mode = "rate_limit"
    elif any(w in err_str for w in ["401", "unauthorized", "session", "permissionerror", "invalid access token", "token expired"]):
        fallback_msg = (
            "आपका लॉगिन सत्र समाप्त हो गया है। कृपया जारी रखने के लिए ऊपर दाईं ओर लॉगिन बटन से पुनः लॉगिन करें।"
            if is_hi else
            "Your login session has expired. Please log in again using your registered mobile number via the Login button to continue."
        )
        mode = "auth_required"
    elif any(w in err_str for w in ["context_length", "maximum context"]):
        fallback_msg = (
            "बातचीत की लंबाई सीमा से अधिक हो गई है। कृपया एक नया प्रश्न पूछें।"
            if is_hi else
            "This conversation has exceeded the maximum length. Please ask a concise question or start a fresh query."
        )
        mode = "context_limit"
    else:
        fallback_msg = (
            "क्षमा करें, सर्वर से संपर्क नहीं हो पा रहा है। कृपया थोड़ी देर बाद पुनः प्रयास करें या सहायता केंद्र से संपर्क करें।"
            if is_hi else
            "I am currently experiencing a temporary server issue. Please try again in a moment or contact the municipal helpdesk."
        )
        mode = "error"

    audio_output = text_to_speech(fallback_msg, user_language)
    return {
        "response": fallback_msg,
        "lang": user_language,
        "mode": mode,
        "audio": audio_output
    }


def validate_and_sanitize_name(name: Any) -> Optional[str]:
    """Validates and sanitizes citizen name extracted by LLM.

    Security & Safety Rules:
        - Must be a string with length between 2 and 40 characters.
        - Must only contain English letters, Devanagari characters, spaces, hyphens, or apostrophes.
        - Rejects prompt injection keywords, commands, instruction markers, and code punctuation.
        - Preserves mixed-case names (e.g. 'McDonald') without destructive .capitalize().

    Args:
        name (Any): The raw candidate name extracted from the user's message.

    Returns:
        str | None: Cleaned and formatted name if valid, otherwise None.
    """
    if not isinstance(name, str):
        return None
    clean = name.strip()
    if len(clean) < 2 or len(clean) > 40:
        return None

    # Reject instruction-like or prompt injection patterns
    suspicious_patterns = [
        r'\bignore\b', r'\binstructions?\b', r'\bsystem\b', r'\bprompt\b',
        r'\bassistant\b', r'\bdelete\b', r'\bdrop\b', r'\bexec\b', r'\bselect\b',
        r'\badmin\b', r'\broot\b', r'\bpassword\b', r'\btoken\b', r'\bcurl\b',
        r'[\{\}\<\>\[\]\(\);\\\/\|\*\&\^\%\$\#\@\!]'
    ]
    for pattern in suspicious_patterns:
        if re.search(pattern, clean, re.IGNORECASE):
            logger.warning(f"[Security] Rejected suspicious extracted citizen name: '{clean}'")
            return None

    # Only allow letters (English + Devanagari), space, hyphen, and apostrophe
    if not re.match(r'^[\sa-zA-Z\u0900-\u097F\'.\-]+$', clean):
        logger.warning(f"[Security] Name contains disallowed characters: '{clean}'")
        return None

    # Preserve casing for mixed-case names (e.g. McDonald, O'Connor), title-case lowercase
    words = [w.strip() for w in clean.split() if w.strip()]
    formatted = []
    for w in words:
        if w.islower():
            formatted.append(w.capitalize())
        else:
            formatted.append(w)
    return " ".join(formatted)


# ====================================================================
# 11. UNIFIED INTENT DISPATCHER (Strategy Pattern)
# ====================================================================

def dispatch_chat_request(
    user_input: str,
    user_language: str,
    detected_script: str,
    history: list,
    session_id: str,
    phone_anchor: str,
    is_authenticated: bool,
    cached_info: Optional[dict],
    workflows: dict,
    services_registry: list,
    system_config: dict
) -> dict:
    """Centralized dispatcher that processes citizen intent, resolves active workflows,
    handles interruptions, and executes either workflow plugins or knowledge base RAG.

    Args:
        user_input (str): The raw text message entered by the citizen.
        user_language (str): Target language code ('en' or 'hi').
        detected_script (str): Script detection ('devanagari', 'latin', etc.).
        history (list): Recent conversation turn history as list of dicts.
        session_id (str): Chat session UUID.
        phone_anchor (str): Phone number or user identifier anchoring the session.
        is_authenticated (bool): Whether the user is verified/authenticated.
        cached_info (dict | None): User profile data (name, phone) if available.
        workflows (dict): Mapping of registered LangGraph plugin workflows.
        services_registry (list): Registry of configured municipal service plugins.
        system_config (dict): Global configuration parsed from config.yml.

    Returns:
        dict: Standardized chat response object:
            {
                "response": str,      # Text response to be displayed to user
                "lang": str,          # Language code of response ('en', 'hi')
                "mode": str,          # Response mode: 'greeting', 'blocked', 'faq',
                                      # 'workflow', 'auth_required', 'rate_limit',
                                      # 'context_limit', or 'error'
                "audio": str | None   # Base64 encoded audio bytes for voice playback
            }

    Side Effects:
        - Redis User Profile: Updates citizen's preferred name when introduced and validated.
        - Redis Pending Interruption: Manages mid-workflow interruption states.
        - Draft Auto-Save: Automatically saves active draft to Qdrant memory when citizen asks an FAQ.
        - LangGraph Checkpointer: Updates workflow checkpoint state for transactional dialogs.
    """
    from services.voice_service import text_to_speech
    from services.intent_service import classify_intent, build_greeting_response
    from storage.redis_manager import save_user_profile_name

    phone = phone_anchor if (phone_anchor and phone_anchor != "default") else session_id
    history_key = phone_anchor if (phone_anchor and phone_anchor != "default") else session_id

    # 1. Multi-Draft Selection Menu Interception (if citizen is selecting an option)
    is_multi_handled, multi_resp = handle_pending_multi_draft(user_input, user_language, phone, session_id, workflows)
    if is_multi_handled:
        return multi_resp

    # 2. Fast-Path Keyword Matching (bypass expensive LLM call for exact greetings & login queries)
    clean_in = re.sub(r'[\s_]+', ' ', user_input.strip().lower())
    greeting_kws = [k.lower() for k in system_config.get("system_messages", {}).get("greeting", {}).get("keywords", [])]
    if not greeting_kws:
        greeting_kws = ["hello", "hi", "hey", "namaste", "नमस्ते", "हेलो"]

    login_cfg = system_config.get("system_messages", {}).get("login", {})
    login_query_kws = [k.lower() for k in login_cfg.get("keywords_query", ["how to login", "how do i login", "login kaise kare", "login help"])]
    login_claim_kws = [k.lower() for k in login_cfg.get("keywords_claim", ["login done", "logged in", "login ho gaya", "maine login kar liya"])]

    special_intent = detect_special_intents(user_input)
    if special_intent:
        intent_data = special_intent
    elif clean_in in greeting_kws:
        intent_data = {
            "intent": "greeting",
            "service": None,
            "emotion": "neutral",
            "extracted_name": None,
            "is_question": False,
            "reasoning": "fast-path greeting keyword match"
        }
    elif any(clean_in == kw or clean_in == f"how to {kw}" for kw in login_query_kws):
        intent_data = {
            "intent": "login_query",
            "service": None,
            "emotion": "neutral",
            "extracted_name": None,
            "is_question": True,
            "reasoning": "fast-path login query keyword match"
        }
    elif any(clean_in == kw for kw in login_claim_kws):
        intent_data = {
            "intent": "login_claim",
            "service": None,
            "emotion": "neutral",
            "extracted_name": None,
            "is_question": False,
            "reasoning": "fast-path login claim keyword match"
        }
    else:
        intent_data = classify_intent(user_input, history, user_language)

    # 3. Save citizen name if introduction was extracted and passes security validation
    extracted_name = intent_data.get("extracted_name")
    validated_name = validate_and_sanitize_name(extracted_name)
    if validated_name:
        save_user_profile_name(history_key, validated_name)

    intent = intent_data.get("intent", "faq")
    logger.info(f"INTENT: {intent}, SERVICE: {intent_data.get('service')}, EMOTION: {intent_data.get('emotion')}")

    # 4. Active Module & Target Plugin Resolution (Dialogue State First)
    is_question = bool(intent_data.get("is_question", False)) or (intent == "faq")
    config = {"configurable": {"thread_id": phone}}
    active_plugin, plugin_intent, target_wf = resolve_active_and_target_plugin(
        user_input, intent, phone, session_id, workflows, services_registry, config, is_question=is_question
    )

    # 5. Service / Workflow Cancellation Intent (adv_cancel, grievance_cancel, booking_cancel)
    cancel_intents = {"adv_cancel", "grievance_cancel", "booking_cancel"}
    if intent in cancel_intents:
        intended_plugin = "adv_booking" if ("adv" in intent or "booking" in intent) else ("grievance" if "grievance" in intent else None)

        # Mismatch guard: If user is actively filling one workflow (e.g. grievance) but classifier
        # emitted a cancel intent for another workflow (e.g. adv_cancel), do NOT wipe the active workflow!
        if active_plugin and intended_plugin and active_plugin != intended_plugin:
            logger.warning(
                f"[WorkflowCancel] Mismatch ignored: user active in '{active_plugin}' but received cancel intent '{intent}' for '{intended_plugin}'."
            )
            name_en = WORKFLOW_METADATA.get(intended_plugin, {}).get("display_name", intended_plugin)
            name_hi = WORKFLOW_METADATA.get(intended_plugin, {}).get("display_name_hi", intended_plugin)
            active_name_en = WORKFLOW_METADATA.get(active_plugin, {}).get("display_name", active_plugin)
            active_name_hi = WORKFLOW_METADATA.get(active_plugin, {}).get("display_name_hi", active_plugin)
            msg = (
                f"You do not have an active {name_en} application in progress. Would you like to continue with your {active_name_en}?"
                if user_language != 'hi' else
                f"आपके पास कोई सक्रिय {name_hi} आवेदन नहीं है। क्या आप अपने {active_name_hi} को जारी रखना चाहते हैं?"
            )
            audio = text_to_speech(msg, user_language)
            return {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

        target_cancelled_plugin = active_plugin or intended_plugin or "adv_booking"
        meta = WORKFLOW_METADATA.get(target_cancelled_plugin, {})
        plugin_display = meta.get("display_name", "Application")
        plugin_display_hi = meta.get("display_name_hi", "आवेदन")

        sys_cancel = (system_config or {}).get("system_messages", {}).get("cancellation", {})
        target_cancel_cfg = sys_cancel.get(target_cancelled_plugin, {})

        if active_plugin and active_plugin in workflows:
            draft_key = get_workflow_draft_key(active_plugin)
            if draft_key:
                empty_draft = get_workflow_reset_draft(active_plugin, cancelled=True)
                workflows[active_plugin].update_state(config, {draft_key: empty_draft, "missing_fields": []})
            try:
                MemoryManager.delete_draft_state(phone, active_plugin)
            except Exception as e:
                logger.warning(f"[WorkflowCancel] Qdrant delete draft failed: {e}")
            clear_pending_interruption(phone)
            logger.info(f"[WorkflowCancel] Cleanly cancelled active workflow {active_plugin} for {phone}")

            if user_language == 'hi':
                msg = target_cancel_cfg.get("hi") or f"आपकी {plugin_display_hi} रद्द कर दी गई है। पोर्टल पर कोई आवेदन दर्ज नहीं किया गया है।\n\nमैं आपकी और क्या सहायता कर सकता हूँ?"
            else:
                msg = target_cancel_cfg.get("en") or f"Your {plugin_display} application has been cancelled. No submission was made to the portal.\n\nHow else can I assist you today?"
        else:
            if user_language == 'hi':
                msg = sys_cancel.get("general", {}).get("hi") or f"ठीक है, {plugin_display_hi} अनुरोध रद्द कर दिया गया है। मैं आपकी और क्या सहायता कर सकता हूँ?"
            else:
                msg = sys_cancel.get("general", {}).get("en") or f"Understood. The {plugin_display.lower()} request has been cancelled. How else can I assist you today?"

        audio = text_to_speech(msg, user_language)
        return {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    # 6. Pure greeting intent (only when NOT in an active transactional workflow)
    if not active_plugin and intent == "greeting":
        greet_msg = build_greeting_response(user_language, workflows)
        audio_output = text_to_speech(greet_msg, user_language)
        return {"response": greet_msg, "lang": user_language, "mode": "greeting", "audio": audio_output}

    # 7. Out-of-domain conversational queries (Semantic Domain Filter - only when NOT in an active workflow)
    if not active_plugin and intent == "out_of_domain":
        from services.rag_service import get_rejection_message
        sys_blocked = (system_config or {}).get("system_messages", {}).get("blocked", {})
        if user_language == 'hi':
            blocked_msg = sys_blocked.get("response_hi") or get_rejection_message("out_of_domain", "hi")
        else:
            blocked_msg = sys_blocked.get("response_en") or get_rejection_message("out_of_domain", "en")
        audio_output = text_to_speech(blocked_msg, user_language)
        return {"response": blocked_msg, "lang": user_language, "audio": audio_output, "mode": "blocked"}

    # 8. Login guidance & claim verification
    if intent in ["login_query", "login_claim"]:
        is_login_handled, login_resp = handle_login_intent(user_input, user_language, is_authenticated, cached_info, detected_intent=intent)
        if is_login_handled:
            return login_resp

    # 9. Mid-Workflow Interruption / FAQ
    ui_lower = user_input.strip().lower()
    is_ui_payload = (ui_lower.startswith("[") and ui_lower.endswith("]")) or (ui_lower.startswith("{") and ui_lower.endswith("}"))
    is_interrupted, faq_resp = handle_workflow_interruption_or_faq(
        user_input, user_language, history, session_id, phone, active_plugin, workflows, config, is_ui_payload, intent_data=intent_data
    )
    if is_interrupted:
        return faq_resp

    # 10. Explicit Draft Management Operations (Continue, Save, Resume, Delete)
    is_draft_op, draft_resp = handle_draft_management(
        intent, user_input, user_language, phone, session_id, active_plugin, workflows, config, system_config=system_config
    )
    if is_draft_op:
        return draft_resp

    # 11. Transactional Workflow Execution (LangGraph Plugins)
    # Read workflow_intents from system_config (config.yml) with safe fallback
    wf_intents_cfg = (system_config or {}).get("system_messages", {}).get("workflow_intents") or DEFAULT_WORKFLOW_INTENTS
    workflow_intents = set(wf_intents_cfg)

    is_query_question = is_informational_question(user_input, intent_data, system_config=system_config)

    has_genuine_tracking_id = any(
        _has_valid_tracking_id(clean_in, srv.get("id_prefixes", []))
        for srv in services_registry
    )

    should_execute_wf = bool(target_wf and target_wf in workflows)
    if is_query_question or intent == "faq":
        if not has_genuine_tracking_id:
            should_execute_wf = False
    elif not active_plugin and intent not in workflow_intents and not has_genuine_tracking_id:
        should_execute_wf = False

    if should_execute_wf:
        logger.info(f"Routing to dynamic plugin: {target_wf}")
        return execute_workflow_plugin(
            target_wf, user_input, user_language, phone, session_id, is_authenticated
        )

    # 12. Knowledge Base RAG / FAQ Retrieval Fallback
    return execute_rag_fallback(user_input, user_language, detected_script, history, session_id)

