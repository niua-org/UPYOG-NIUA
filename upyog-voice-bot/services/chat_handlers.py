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
from typing import Dict, Any, Optional, Tuple, List
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
GROQ_MODEL = os.environ.get("GROQ_MODEL", "openai/gpt-oss-20b")


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

def handle_login_intent(user_input: str, user_language: str, is_authenticated: bool, cached_info: Optional[dict]) -> Tuple[bool, Optional[dict]]:
    """Handles login guidance and login claim queries."""
    from services.voice_service import text_to_speech

    ui_clean = user_input.lower().strip()
    login_kws = [
        "how to login", "how to log in", "how do i login", "how can i login",
        "login kaise kare", "login kaise karte hain", "login process",
        "login process kya hai", "can you login", "log me in", "where is login",
        "login option", "login button", "login kaise hoga", "login kahan hai",
        "login kaise karein", "login kaise karey", "login steps", "login karna",
        "login kaise kiya jata hai", "login karna hai", "can i login without clicking",
        "login without clicking", "how to sign in", "sign in kaise kare"
    ]
    is_login_query = (
        any(kw in ui_clean for kw in login_kws) or
        (("login" in ui_clean or "log in" in ui_clean or "sign in" in ui_clean) and
         any(w in ui_clean for w in ["how", "kaise", "where", "kahan", "procedure", "karna", "process", "steps", "help", "batao", "bataiye", "can you", "without", "bina"]))
    )
    if is_login_query:
        if user_language == "hi":
            login_msg = (
                "बाईं ओर के साइडबार (Left Sidebar) में नीचे जाएं और **Login** विकल्प पर क्लिक करें। "
                "अपना पंजीकृत मोबाइल नंबर भरें और फिर प्राप्त OTP दर्ज करें। "
                "लॉगिन करने के बाद, आप बुकिंग बना सकते हैं, शिकायत दर्ज कर सकते हैं और अपने आवेदन की स्थिति देख सकते हैं।"
            )
        else:
            login_msg = (
                "In the left sidebar, scroll down and click on **Login**. "
                "Enter your registered mobile number and then enter the OTP received on your phone. "
                "Once logged in, you will be able to create bookings, register complaints, and check your application status."
            )
        audio_output = text_to_speech(login_msg, user_language)
        return True, {"response": login_msg, "lang": user_language, "mode": "faq", "audio": audio_output}

    login_claim_kws = [
        "logging done", "login done", "logged in", "i have logged in",
        "i logged in", "done", "login ho gaya", "maine login kar liya",
        "login complete", "login kar liya", "login hogaya", "signed in",
        "i have signed in", "now logged in", "login completed", "done login",
        "login kar chuka hu", "login ho chuka hai"
    ]
    is_login_claim = any(ui_clean == kw or ui_clean.startswith(kw) for kw in login_claim_kws)
    if is_login_claim:
        if is_authenticated:
            user_name = (cached_info.get("name") if cached_info else None) or "Citizen"
            if user_language == "hi":
                resp_msg = f"बहुत बढ़िया! आपकी पहचान सत्यापित हो गई है ({user_name})। अब आप विज्ञापन बुकिंग कर सकते हैं या शिकायत दर्ज कर सकते हैं। आप क्या करना चाहते हैं?"
            else:
                resp_msg = f"Great! Your login session is verified ({user_name}). You can now proceed to book an advertisement or register a complaint. How would you like to proceed?"
        else:
            if user_language == "hi":
                resp_msg = "मुझे अभी आपका सक्रिय लॉगिन सत्र नहीं मिला है। कृपया बाईं ओर के साइडबार में नीचे **Login** विकल्प पर क्लिक करके अपने मोबाइल नंबर और OTP से लॉगिन पूरा करें।"
            else:
                resp_msg = "I do not detect an active logged-in session yet. Please complete the login by clicking **Login** in the left sidebar and entering your mobile number and OTP."
        audio_output = text_to_speech(resp_msg, user_language)
        return True, {"response": resp_msg, "lang": user_language, "mode": "faq", "audio": audio_output}

    return False, None


def detect_special_intents(user_input: str) -> Optional[dict]:
    """Detects draft operations and profile queries before LLM intent classifier."""
    ui_lower = user_input.strip().lower()

    if (ui_lower.startswith("[") and ui_lower.endswith("]")) or (ui_lower.startswith("{") and ui_lower.endswith("}")):
        return {"intent": "none", "service": "None", "emotion": "neutral"}

    profile_triggers = [
        "profile", "my profile", "profile details", "who am i", "what is my name",
        "show my details", "my details", "account details", "my account",
        "user details", "mera profile", "meri profile", "mera naam", "meri details",
        "profile info", "my info", "account info"
    ]
    if any(re.search(rf'\b{re.escape(w)}\b', ui_lower) for w in profile_triggers):
        return {"intent": "profile", "service": "None", "emotion": "neutral"}

    draft_delete_patterns = [
        r"\b(delete|remove|clear|discard|cancel|erase|drop)\s+(all\s+)?(my\s+)?(saved\s+)?(drafts?|applications?)\b",
        r"\b(my\s+)?(saved\s+)?(drafts?|applications?)\s+(ko\s+)?(delete|cancel|clear|discard|remove|hatao|hata\s*do|hataiye|mitado)\b",
        r"\b(delete|remove|clear|discard|cancel)\s+(the\s+)?drafts?\b",
        r"\b(delete|remove|clear|discard|cancel)\s+(draft\s*\d+|option\s*\d+|\d+)\b",
        r"\b(delete|remove|clear|discard|cancel)\s+(grievance|complaint|booking|advertisement|adv)\s+draft\b",
        r"\bdraft(s)?\s+(delete|cancel|clear|discard|remove|hatao|hata\s*do|hataiye|khatam)\b",
        r"\b(mere|mera|sab|saare)\s+draft(s)?\s+(delete|cancel|clear|hatao|hata\s*do|hataiye)\b",
        r"\bdelete\s+(all\s+)?drafts?\b",
        r"\bcancel\s+(all\s+)?drafts?\b",
        r"\bclear\s+(all\s+)?drafts?\b",
        r"\bdiscard\s+(all\s+)?drafts?\b",
        r"\bcancel\s+application\b",
        r"\bcancel\s+my\s+application\b",
        r"\bdelete\s+my\s+application\b",
        r"\bdelete\s+application\b"
    ]
    if any(re.search(p, ui_lower) for p in draft_delete_patterns):
        return {"intent": "draft_delete", "service": "None", "emotion": "neutral"}

    draft_continue_patterns = [
        r"\b(continue|resume)\s+(my\s+)?(application|booking|complaint|draft|grievance)\b",
        r"\b(aage\s+badhao|jaari\s+rakhein|continue\s+karo)\b",
        r"^continue$",
        r"^resume$"
    ]
    if any(re.search(p, ui_lower) for p in draft_continue_patterns):
        return {"intent": "draft_continue_application", "service": "None", "emotion": "neutral"}

    draft_save_patterns = [
        r"\bsave\s+(the\s+|my\s+)?(draft|application)\b",
        r"\bdraft\s+save\s*(karo|kar\s*do|karein)?\b"
    ]
    if any(re.search(p, ui_lower) for p in draft_save_patterns):
        return {"intent": "draft_save", "service": "None", "emotion": "neutral"}

    draft_view_patterns = [
        r"\b(show|view|list|check|see|get|fetch|display|open|dikhao|batao|bataiye)\s+(all\s+)?(my\s+)?(saved\s+)?(drafts?)\b",
        r"\b(my\s+|saved\s+|all\s+|mere\s+|mera\s+)?drafts?\s+(list|dikhao|batao|bataiye|dekho|dekhein)\b",
        r"^(show\s+)?(my\s+)?drafts?$",
        r"^(saved\s+)?drafts?$",
        r"^(mere\s+|mera\s+)?drafts?(\s+dikhao)?$",
        r"\b(show|view|list|open)\s+drafts?\b"
    ]
    if any(re.search(p, ui_lower) for p in draft_view_patterns) or (
        "draft" in ui_lower and any(w in ui_lower for w in ["show", "resume", "continue", "open", "list", "view", "check", "dikhao", "batao"])
    ):
        return {"intent": "draft_resume", "service": "None", "emotion": "neutral"}

    if "end conversation" in ui_lower:
        return {"intent": "end_conversation", "service": "None", "emotion": "neutral"}

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

def resolve_active_and_target_plugin(user_input: str, intent: str, phone: str, session_id: str, workflows: dict, services_registry: list, config: dict) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    """
    Identifies active module and matches incoming message to target plugin.
    Handles service switching with draft auto-save.
    Returns: (active_plugin, plugin_intent, target_wf)
    """
    active_plugin = None
    active_plugins = []
    for wf_name, graph in workflows.items():
        state = graph.get_state(config)
        if state and state.values:
            draft_key = "draft_booking" if wf_name == "adv_booking" else "draft_grievance"
            draft = state.values.get(draft_key)
            if isinstance(draft, dict) and not draft.get("_cancelled"):
                has_fields = any(v for k, v in draft.items() if not k.startswith("_") and v is not None and str(v).strip() != "")
                has_options = bool(draft.get("_category_options") or draft.get("_sub_options") or draft.get("_locality_options") or draft.get("_slot_options"))
                messages = state.values.get("messages", [])

                if (has_fields or has_options) and messages:
                    timestamp = getattr(state, "created_at", "") or ""
                    active_plugins.append((wf_name, timestamp, len(messages)))

    if active_plugins:
        active_plugins.sort(key=lambda x: (x[1], x[2]), reverse=True)
        active_plugin = active_plugins[0][0]

    plugin_intent = None
    ui_lower = user_input.lower()

    for srv in services_registry:
        s_key = srv.get("key")
        if s_key not in workflows:
            continue
        keywords = [k.lower() for k in srv.get("keywords", [])]
        id_prefixes = [p.lower() for p in srv.get("id_prefixes", [])]

        has_id = any(p in ui_lower for p in id_prefixes)
        has_kw = any(re.search(rf'\b{re.escape(w)}\b', ui_lower) for w in keywords)

        if has_id or has_kw:
            plugin_intent = s_key
            break

    if not plugin_intent and intent != "profile":
        if intent in ["grievance_candidate", "grievance_status_candidate"]:
            plugin_intent = "grievance"
        elif intent in ["adv_candidate", "adv_status_candidate", "booking_candidate"]:
            plugin_intent = "adv_booking"
        elif intent in ["adv_confirm", "booking_confirm", "adv_cancel", "booking_cancel", "grievance_confirm", "grievance_cancel"]:
            plugin_intent = active_plugin or ("adv_booking" if "adv" in intent or "booking" in intent else "grievance")

    is_generic_response = ui_lower in ["yes", "no", "yeah", "yup", "ya", "ok", "okay", "sure", "confirm", "cancel", "haan", "nahi", "nahin", "1", "2", "option 1", "option 2"]
    if plugin_intent and active_plugin and plugin_intent != active_plugin and not is_generic_response:
        logger.info(f"[Router] User switched service: {active_plugin} -> {plugin_intent}")
        if active_plugin in workflows:
            prev_state = workflows[active_plugin].get_state(config)
            if prev_state and prev_state.values:
                prev_draft_key = "draft_booking" if active_plugin == "adv_booking" else "draft_grievance"
                prev_draft = prev_state.values.get(prev_draft_key) or {}
                if prev_draft and any(v for k, v in prev_draft.items() if not k.startswith("_") and v is not None and str(v).strip() != ""):
                    from storage.qdrant_manager import MemoryManager
                    MemoryManager.save_draft_state(phone, active_plugin, prev_draft)
                    logger.info(f"[AutoSave] Saved previous workflow draft for {phone}/{active_plugin}")

                empty_draft = {f: None for f in (["category", "sub_category", "description", "locality"] if active_plugin == "grievance" else ["addType", "location", "faceArea", "start_date", "end_date", "nightLight"])}
                workflows[active_plugin].update_state(config, {prev_draft_key: empty_draft, "missing_fields": [], "messages": []})
        active_plugin = plugin_intent
    elif active_plugin and is_generic_response:
        plugin_intent = active_plugin

    target_wf = plugin_intent or active_plugin
    return active_plugin, plugin_intent, target_wf


# ====================================================================
# 6. WORKFLOW INTERRUPTION & MID-FLOW FAQ
# ====================================================================

def handle_workflow_interruption_or_faq(user_input: str, user_language: str, history: list, session_id: str, phone: str, active_plugin: Optional[str], workflows: dict, config: dict, is_ui_payload: bool) -> Tuple[bool, Optional[dict]]:
    """Detects mid-workflow questions, auto-saves active draft, and responds with FAQ."""
    if not active_plugin or active_plugin not in workflows or is_ui_payload:
        return False, None

    question_triggers = [
        "?", "what is", "what does", "explain", "meaning", "kya hai", "kaise", "kyun",
        "tell me about", "rules for", "how to", "who is", "help with", "information about",
        "where can", "details of", "procedure for", "charges for", "fees for", "kya hota",
        "kaun", "kahan", "batao", "bataiye", "jaankari", "information"
    ]
    is_explicit_question = any(q in user_input.lower() for q in question_triggers)

    is_option_selection = False
    if not is_explicit_question:
        state = workflows[active_plugin].get_state(config)
        if state and state.values:
            draft_key = "draft_booking" if active_plugin == "adv_booking" else "draft_grievance"
            draft = state.values.get(draft_key) or {}

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
            clean_in = re.sub(r'[\s_]+', '', user_input.strip().lower())
            is_digit_choice = bool(re.match(r'^(?:option\s*)?\d+$', user_input.strip().lower()))

            if is_digit_choice or clean_in in all_opts or any(clean_in == o or (o in clean_in and len(user_input.split()) <= 3) for o in all_opts):
                is_option_selection = True

    if is_explicit_question and not is_option_selection:
        from services.rag_service import retrieve_document
        from services.voice_service import text_to_speech
        from storage.qdrant_manager import MemoryManager

        draft_saved = False
        saved_plugin_name = None

        state = workflows[active_plugin].get_state(config)
        if state and state.values:
            draft_key = "draft_booking" if active_plugin == "adv_booking" else "draft_grievance"
            draft = state.values.get(draft_key) or {}
            real_fields = {k: v for k, v in draft.items() if not k.startswith("_") and v is not None and str(v).strip() != ""}
            if real_fields:
                MemoryManager.save_draft_state(phone, active_plugin, draft)
                draft_saved = True
                saved_plugin_name = active_plugin

            empty_draft = {f: None for f in (["category", "sub_category", "description", "locality"] if active_plugin == "grievance" else ["addType", "location", "faceArea", "start_date", "end_date", "nightLight"])}
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

def handle_draft_management(intent: str, user_input: str, user_language: str, phone: str, session_id: str, active_plugin: Optional[str], workflows: dict, config: dict) -> Tuple[bool, Optional[dict]]:
    """Handles explicit draft operations (continue, save, resume, delete, profile)."""
    from storage.qdrant_manager import MemoryManager
    from services.voice_service import text_to_speech
    from services.intent_service import process_user_message
    from draft_switcher import MultiDraftSwitcher, _SERVICES_MAP
    ui_lower = user_input.lower()

    if intent == "draft_continue_application":
        all_drafts = MemoryManager.get_all_draft_states(phone)
        if len(all_drafts) == 1:
            target_wf = all_drafts[0].get("plugin_name", "adv_booking")
            draft_data = all_drafts[0].get("draft_data", {})
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
                draft_key = "draft_booking" if plugin == "adv_booking" else "draft_grievance"
                draft = state.values.get(draft_key) or {}
                if draft:
                    MemoryManager.save_draft_state(phone, plugin, draft)
        msg = "Your draft application has been saved successfully. You can resume it anytime by saying 'Continue my application'!"
        audio = text_to_speech(msg, user_language)
        return True, {"response": msg, "lang": user_language, "mode": "faq", "audio": audio}

    if intent == "end_conversation":
        clear_pending_interruption(phone)
        msg = "Goodbye! Have a great day!"
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

    agent_res = process_user_message(user_input, phone, session_id, target_workflow=target_wf)
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
