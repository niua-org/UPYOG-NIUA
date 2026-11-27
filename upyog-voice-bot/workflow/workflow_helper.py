import logging
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

from langchain_core.messages import AIMessage
from langgraph.graph import END

logger = logging.getLogger(__name__)

CONSENT_YES_KEYWORDS = {
    "yes", "proceed", "yes, proceed", "yes proceed", "sure", "ok", "okay", "start",
    "haan", "ha", "chalo", "1", "option 1", "yes please", "please proceed",
    "हाँ", "आगे बढ़ें", "हाँ, आगे बढ़ें", "शुरू करें", "आगे चलो",
}

CONSENT_NO_KEYWORDS = {
    "no", "cancel", "no, cancel", "no cancel", "exit", "quit", "nahi", "nahin",
    "rehne do", "stop", "abort", "2", "option 2", "don't proceed", "dont proceed",
    "नहीं", "रद्द करें", "नहीं, रद्द करें", "बंद करें", "रद्द",
}

DEFAULT_INITIATION_TRIGGERS = {
    "en": ["help", "book", "file", "register", "start", "new", "apply", "create"],
    "hi": ["मदद", "सहायता", "बुक", "दर्ज", "शिकायत", "आवेदन", "शुरू", "नया", "नई", "karna", "darj"],
}

_SKIP_MEMORY_MESSAGES = {"continue", "hello", "[CANCEL_DRAFT]"}


# ==========================================
# CONFIG & LANGUAGE HELPERS
# ==========================================

def _get_system_config() -> dict:
    try:
        from services.intent_service import _load_system_config
        return _load_system_config() or {}
    except Exception:
        return {}


def _resolve_language(state: Dict[str, Any], user_msg: str = "") -> str:
    """Resolves 'hi' or 'en' from workflow state or Devanagari script detection."""
    lang = state.get("user_language")
    if lang in ("hi", "en"):
        return lang
    return "hi" if any("\u0900" <= c <= "\u097f" for c in (user_msg or "")) else "en"


# ==========================================
# CONSENT & INITIATION DETECTION
# ==========================================

def parse_consent_response(user_msg: str, user_language: str = "en") -> Optional[bool]:
    """Parses citizen response to the workflow consent prompt.

    Returns:
        True: User consented to start/proceed with the workflow.
        False: User declined or cancelled.
        None: Ambiguous or answering a form field directly.
    """
    msg = (user_msg or "").strip().lower()
    if not msg:
        return None

    if msg in CONSENT_NO_KEYWORDS or any(msg.startswith(w) for w in ("no", "cancel", "nahi", "nahin", "stop", "abort")):
        return False

    if msg in CONSENT_YES_KEYWORDS or any(msg.startswith(w) for w in ("yes", "proceed", "haan", "sure", "start")):
        return True

    return None


def get_workflow_consent_options(user_language: str = "en") -> List[str]:
    """Returns localized consent dropdown options dynamically from config.yml."""
    cfg = _get_system_config()
    lang = "hi" if user_language == "hi" else "en"
    opts = cfg.get("system_messages", {}).get("workflow_templates", {}).get("consent_options", {}).get(lang)
    if opts and isinstance(opts, list):
        return opts
    return ["हाँ, आगे बढ़ें", "नहीं, रद्द करें"] if lang == "hi" else ["Yes, proceed", "No, cancel"]


def get_workflow_cancellation_message(workflow_key: str, user_language: str = "en") -> str:
    """Returns localized workflow cancellation confirmation message from config.yml."""
    cfg = _get_system_config()
    lang = "hi" if user_language == "hi" else "en"
    cancel_cfg = cfg.get("system_messages", {}).get("cancellation", {})
    srv_cfg = cancel_cfg.get(workflow_key) or cancel_cfg.get("general") or {}
    if srv_cfg.get(lang):
        return srv_cfg[lang]
    return (
        "अनुरोध रद्द कर दिया गया है। पोर्टल पर कोई आवेदन दर्ज नहीं किया गया है।"
        if lang == "hi"
        else "Your request has been cancelled. No submission was made to the portal."
    )


def is_workflow_initiation(user_msg: str, workflow_key: str = "") -> bool:
    """Detects whether a citizen message is an explicit service initiation request."""
    msg = (user_msg or "").strip().lower()
    if not msg:
        return False

    cfg = _get_system_config()
    triggers_cfg = (
        cfg.get("system_messages", {})
        .get("workflow_templates", {})
        .get("initiation_triggers", {})
    )
    en_triggers = triggers_cfg.get("en") or DEFAULT_INITIATION_TRIGGERS["en"]
    hi_triggers = triggers_cfg.get("hi") or DEFAULT_INITIATION_TRIGGERS["hi"]
    all_triggers = [t.lower() for t in (en_triggers + hi_triggers)]

    words = msg.replace(",", " ").replace(".", " ").replace("?", " ").split()
    word_set = set(words)

    has_trigger = any(
        trigger in word_set or any(w.startswith(trigger) for w in words)
        for trigger in all_triggers
    )
    if not has_trigger:
        has_trigger = any(trigger in msg for trigger in all_triggers if " " in trigger)
    if not has_trigger:
        return False

    if not workflow_key:
        return True

    srv = next((s for s in cfg.get("services", []) if s.get("key") == workflow_key), {})
    kw_list = [k.lower() for k in srv.get("keywords", [])]
    srv_name = (srv.get("name") or "").lower()
    srv_name_hi = (srv.get("name_hi") or "").lower()
    return any(kw in msg for kw in kw_list) or (srv_name in msg) or (srv_name_hi in msg)


# ==========================================
# INTRO MESSAGE & SCHEMA EXTRACTION
# ==========================================

def _format_date_val(val: Any) -> str:
    try:
        return datetime.strptime(str(val).strip(), "%Y-%m-%d").strftime("%d %b %Y")
    except Exception:
        return str(val).strip()


def _extract_display_items(form_fields: List[dict], draft: Dict[str, Any], lang: str) -> List[tuple]:
    """Extracts human-readable (field_id, label, value) tuples from draft using form schema."""
    items: List[tuple] = []
    for f in form_fields:
        f_id = f.get("id")
        val = draft.get(f_id)
        if val is None or not str(val).strip() or str(f_id).startswith("_"):
            continue

        if isinstance(val, dict):
            val_disp = val.get("name") or val.get("label") or str(val)
        elif isinstance(val, list):
            val_disp = ", ".join(str(x) for x in val)
        else:
            val_disp = str(val).strip()

        if f.get("type") == "date":
            val_disp = _format_date_val(val_disp)
        if len(val_disp) > 40:
            val_disp = val_disp[:37] + "..."

        raw_label = f.get(f"label_{lang}") or f.get("label") or f_id.replace("_", " ").title()
        clean_label = raw_label.replace("Advertisement ", "").replace("Complaint ", "")
        items.append((f_id, clean_label, val_disp))
    return items


def build_workflow_intro_message(
    workflow_key: str,
    draft: Dict[str, Any],
    user_language: str = "en",
    include_consent_ui: bool = True,
) -> str:
    """Dynamically builds localized acknowledgment, stages overview, and consent prompt from config.yml."""
    cfg = _get_system_config()
    lang = "hi" if user_language == "hi" else "en"

    service_meta = next((s for s in cfg.get("services", []) if s.get("key") == workflow_key), {})
    service_name = (
        (service_meta.get("name_hi") if lang == "hi" else service_meta.get("name"))
        or cfg.get("forms", {}).get(workflow_key, {}).get("name")
        or workflow_key.replace("_", " ").title()
    )

    form_fields = cfg.get("forms", {}).get(workflow_key, {}).get("fields", [])
    extracted_items = _extract_display_items(form_fields, draft, lang)

    intros_cfg = cfg.get("system_messages", {}).get("workflow_intros", {}).get(workflow_key, {})
    stages_text = intros_cfg.get(f"stages_{lang}") or intros_cfg.get("stages_en", "")
    templates = cfg.get("system_messages", {}).get("workflow_templates", {})

    if extracted_items:
        bullet_list = "\n".join(f"• **{label}**: {val}" for _, label, val in extracted_items)
        tmpl_cfg = templates.get("intro_with_extracted", {})
        tmpl = tmpl_cfg.get(lang) or tmpl_cfg.get("en", "")
        format_kwargs = {"service_name": service_name, "extracted_details": bullet_list, "stages": stages_text}
    else:
        tmpl_cfg = templates.get("intro_generic", {})
        tmpl = tmpl_cfg.get(lang) or tmpl_cfg.get("en", "")
        format_kwargs = {"service_name": service_name, "stages": stages_text}

    ack_text = tmpl.format(**format_kwargs)

    if stages_text and "{stages}" not in tmpl and stages_text not in ack_text:
        ack_text = f"{ack_text}\n{stages_text}\n"

    if not include_consent_ui:
        return ack_text.strip()

    consent_opts = get_workflow_consent_options(user_language=lang)
    opts_str = ", ".join(f"'{o}'" for o in consent_opts)
    return f"{ack_text.strip()}\n<ui-dropdown options=[{opts_str}] />"


# ==========================================
# SHARED LANGGRAPH CONSENT NODE & PROMPT
# ==========================================

def make_consent_node(
    workflow_key: str,
    draft_key: str,
    all_fields: List[str],
    cancel_msg_fn: Optional[Callable[[str], str]] = None,
    extra_reset_keys: Optional[List[str]] = None,
):
    """Factory returning a LangGraph consent node for any service workflow."""
    from storage.qdrant_manager import MemoryManager

    def _reset_draft(cancelled: bool = False) -> Dict[str, Any]:
        clean: Dict[str, Any] = {f: None for f in all_fields}
        for k in (extra_reset_keys or []):
            clean[k] = None
        clean.update({
            "_consent_pending": False,
            "_consent_granted": False,
            "_steps_communicated": False,
        })
        if cancelled:
            clean["_cancelled"] = True
        return clean

    def consent_node(state: Dict[str, Any]) -> Dict[str, Any]:
        messages = state.get("messages", [])
        phone_number = state.get("phone_number", "default")
        draft = state.get(draft_key)
        if not isinstance(draft, dict):
            draft = {}
        user_msg = messages[-1].content if messages else ""

        if draft.get("_consent_pending"):
            consent_decision = parse_consent_response(user_msg)
            if consent_decision is False or user_msg.strip() == "[CANCEL_DRAFT]":
                reset = _reset_draft(cancelled=True)
                try:
                    MemoryManager.delete_draft_state(phone_number, workflow_key)
                except Exception as e:
                    logger.warning(f"[{workflow_key}] Qdrant delete draft failed: {e}")

                user_lang = _resolve_language(state, user_msg)
                msg = cancel_msg_fn(user_lang) if cancel_msg_fn else get_workflow_cancellation_message(workflow_key, user_lang)
                return {"messages": [AIMessage(content=msg)], draft_key: reset, "missing_fields": []}

            if consent_decision is True:
                draft["_consent_granted"] = True
                draft["_consent_pending"] = False
                draft["_just_consented"] = True
                logger.info(f"[{workflow_key}] Citizen granted consent -> proceeding")
                return {draft_key: draft, "messages": messages}

        if not draft.get("_consent_pending") and is_workflow_initiation(user_msg, workflow_key):
            logger.info(f"[{workflow_key}] Fresh workflow initiation detected: '{user_msg}' -> resetting draft")
            return {draft_key: _reset_draft(cancelled=False)}

        return {draft_key: draft}

    return consent_node


def consent_router(state: Dict[str, Any]) -> str:
    """Routes to END if the consent node produced a reply, otherwise to extraction."""
    messages = state.get("messages", [])
    if messages and isinstance(messages[-1], AIMessage):
        return END
    return "extraction"


def maybe_build_consent_prompt(
    workflow_key: str,
    draft: Dict[str, Any],
    draft_key: str,
    all_fields: List[str],
    state: Dict[str, Any],
) -> Optional[Dict[str, Any]]:
    """Shows the workflow steps & consent prompt on first turn; returns None once granted."""
    from storage.qdrant_manager import MemoryManager

    user_msg = (state.get("messages") or [None])[-1].content if state.get("messages") else ""
    um_lower = user_msg.strip().lower()

    if draft.get("_consent_granted") or draft.get("_steps_communicated") or "continue" in um_lower or um_lower in ("resume", "[resume_draft]"):
        draft["_consent_granted"] = True
        return None

    phone = state.get("phone_number", "default")
    lang = _resolve_language(state, user_msg)
    draft["_consent_pending"] = draft["_steps_communicated"] = True

    opts = get_workflow_consent_options(user_language=lang)
    intro_msg = build_workflow_intro_message(workflow_key, draft, user_language=lang, include_consent_ui=True)

    if user_msg and user_msg not in _SKIP_MEMORY_MESSAGES:
        MemoryManager.save_long_term_interaction(phone_number=phone, role="user", content=user_msg)
    MemoryManager.save_long_term_interaction(phone_number=phone, role="assistant", content=intro_msg)

    return {
        "messages": [AIMessage(content=intro_msg)],
        draft_key: draft,
        "missing_fields": [f for f in all_fields if not draft.get(f)],
        "input_type": "dropdown",
        "options": opts,
    }
