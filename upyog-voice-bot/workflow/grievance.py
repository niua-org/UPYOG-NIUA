import json
import logging
import re
from typing import Dict, Any, List
import os
import yaml

from langchain_core.messages import SystemMessage, AIMessage, HumanMessage
from langgraph.graph import StateGraph, END, START
from services.llm_client import get_unified_chat_model

from storage.qdrant_manager import MemoryManager, shared_memory
from prompts.workflow_prompts import (
    build_grievance_extract_prompt,
    build_grievance_question_prompt,
)
from services.validator import validate_field
from workflow.base_state import BaseAgentState
from services.nlu_lexicon import (
    is_bare_negation,
    is_start_new_workflow,
    is_explicit_cancel,
    is_affirmative,
    EXCLUDED_FIELD_WORDS
)

logger = logging.getLogger(__name__)

llm = get_unified_chat_model(temperature=0)


# ==========================================
# STATE
# ==========================================

class GrievanceState(BaseAgentState):
    """
    Extends BaseAgentState with grievance-specific fields.

    draft_grievance keys:
      category          -- main complaint category name (display)
      sub_category      -- specific complaint type name (display)
      description       -- free-text problem description
      locality          -- area/locality name (display)
      category_code     -- service code sent to PGR API
      locality_code     -- boundary code sent to PGR API
      _category_options -- cached {group: [{name, code}]} dict from MDMS
      _sub_options      -- cached [{name, code}] for selected category
      _locality_options -- cached [{name, code}] from egov-location
      _awaiting_confirm -- True after confirm_node shows the summary
      _confirmed        -- True when user says yes at confirmation step
      _cancelled        -- True when user says no at confirmation step
    """
    draft_grievance: Dict[str, Any]
    missing_fields: List[str]


# ==========
# CONFIG
# ==========

# _this_dir = os.path.dirname(os.path.abspath(__file__))
# _config_path = os.path.join(os.path.dirname(_this_dir), "config.yml")
# if not os.path.exists(_config_path):
#     _config_path = os.path.join(_this_dir, "config.yml")

# try:
#     with open(_config_path, encoding="utf-8", "r") as _f:
#         _raw_cfg = yaml.safe_load(_f)
# except Exception as e:
#     logger.error(f"Failed to load config.yml in grievance.py: {e}")
#     _raw_cfg = {}

# _grv_form_cfg = _raw_cfg.get("forms", {}).get("grievance", {})
# FORM_FIELDS   = _grv_form_cfg.get("fields", [])
# ALL_FIELDS    = [f["id"] for f in FORM_FIELDS]
# # Only fields explicitly enabled in config.yml are eligible for chat extraction.
# EXTRACTABLE_FIELDS = [f["id"] for f in FORM_FIELDS if f.get("extractable", False)]
# FIELD_HINTS   = {f["id"]: f.get("label", f["id"]) for f in FORM_FIELDS}

def load_and_validate_grievance_config(config_path: str = None, yaml_content: str = None):
    """
    Loads and validates grievance form configuration from config.yml.
    
    Why this is required:
    1. Uses encoding="utf-8" so Windows doesn't crash on Hindi characters (e.g. नमस्ते).
    2. Validates that required fields actually exist instead of silently returning empty lists.
    3. Prevents the bot from ever entering an empty or broken workflow state.
    """
    if yaml_content is not None:
        raw_cfg = yaml.safe_load(yaml_content) or {}
    else:
        if not config_path:
            this_dir = os.path.dirname(os.path.abspath(__file__))
            config_path = os.path.join(os.path.dirname(this_dir), "config.yml")
            if not os.path.exists(config_path):
                config_path = os.path.join(this_dir, "config.yml")

        if not os.path.exists(config_path):
            raise FileNotFoundError(f"Configuration file not found at: {config_path}")

        # Note: "r" must come before encoding="utf-8"
        with open(config_path, "r", encoding="utf-8") as f:
            raw_cfg = yaml.safe_load(f) or {}

    # Check that the grievance form section exists
    forms_cfg = raw_cfg.get("forms")
    if not isinstance(forms_cfg, dict) or "grievance" not in forms_cfg:
        raise ValueError("Configuration error: Missing 'forms.grievance' section in config.yml")

    # Check that grievance fields list is present and non-empty
    grv_form_cfg = forms_cfg.get("grievance", {})
    form_fields = grv_form_cfg.get("fields")
    if not isinstance(form_fields, list) or len(form_fields) == 0:
        raise ValueError("Configuration error: 'forms.grievance.fields' must be a non-empty list")

    # Validate each individual field definition
    seen_ids = set()
    valid_types = {"text", "dropdown", "date", "slots", "file", "number", "choice", "checkbox"}

    for idx, field in enumerate(form_fields):
        if not isinstance(field, dict):
            raise ValueError(f"Configuration error: Field at index {idx} must be a dictionary")

        f_id = field.get("id")
        if not f_id or not isinstance(f_id, str) or not f_id.strip():
            raise ValueError(f"Configuration error: Field at index {idx} is missing a valid 'id'")

        if f_id in seen_ids:
            raise ValueError(f"Configuration error: Duplicate field ID '{f_id}' found at index {idx}")
        seen_ids.add(f_id)

        f_label = field.get("label")
        if not f_label or not isinstance(f_label, str) or not f_label.strip():
            raise ValueError(f"Configuration error: Field '{f_id}' is missing a valid 'label'")

        f_type = field.get("type")
        if not f_type or f_type not in valid_types:
            raise ValueError(f"Configuration error: Field '{f_id}' has an invalid widget type '{f_type}'")

    # Compute lookup variables used by the rest of the workflow
    all_fields = [f["id"] for f in form_fields]
    extractable_fields = [f["id"] for f in form_fields if f.get("extractable", False)]
    field_hints = {f["id"]: f.get("label", f["id"]) for f in form_fields}

    return form_fields, all_fields, extractable_fields, field_hints


# Load and validate configuration at startup
FORM_FIELDS, ALL_FIELDS, EXTRACTABLE_FIELDS, FIELD_HINTS = load_and_validate_grievance_config()



# Checks the draft memory and figures out which complaint detail is missing next
def get_next_step(draft: dict) -> dict:
    if not isinstance(draft, dict):
        draft = {}

    # Prioritize any field that failed validation and is still missing
    for f_id in draft.get("_failed_fields", []):
        if not draft.get(f_id):
            failed_step = next((f for f in FORM_FIELDS if f["id"] == f_id), None)
            if failed_step:
                return failed_step

    for field in FORM_FIELDS:
        if not draft.get(field["id"]):
            return field
    return {}


from mcp_tools import pgr_get_categories, pgr_get_localities, pgr_create_complaint, pgr_search_complaints, pgr_search_complaints_raw

# ==========================================
# LIVE API HELPERS (thin wrappers)
# ==========================================

# Fetches the list of all complaint categories from the UPYOG MDMS database
def _pgr_categories() -> dict:
    try:
        return pgr_get_categories() or {}
    except Exception as e:
        logger.error(f"[grievance] _pgr_categories failed: {e}")
        return {}


# Fetches the list of all valid localities for the city from the UPYOG location database
def _pgr_localities() -> list:
    try:
        return pgr_get_localities() or []
    except Exception as e:
        logger.error(f"[grievance] _pgr_localities failed: {e}")
        return []


# ==========================================
# NODES
# ==========================================

# Checks if the user wants to cancel the current complaint or view past complaints
def intent_and_ui_node(state: GrievanceState):
    messages     = state.get("messages", [])
    phone_number = state.get("phone_number", "default")
    draft        = dict(state.get("draft_grievance", {}))
    user_msg     = messages[-1].content if messages else ""
    ui_lower     = user_msg.lower().strip()

    # CLEAR STALE FLAGS FROM PREVIOUS SESSIONS:
    # Why this is required:
    # If a citizen previously cancelled or completed a complaint, LangGraph's persistent
    # checkpointer retains '_cancelled: True' or '_confirmed: True' in memory for this phone number.
    # When the citizen sends a fresh message, we must clear these flags so the router
    # does not mistakenly cancel or re-submit their new request.

    draft.pop("_cancelled", None)
    draft.pop("_confirmed", None)
    
    # If the draft fields are empty, ensure awaiting confirmation is also cleared
    if not all(draft.get(f) for f in ALL_FIELDS):
        draft.pop("_awaiting_confirm", None)

    # Cancel command from draft management system
    if user_msg == "[CANCEL_DRAFT]":
        draft = {f: None for f in ALL_FIELDS}
        draft.pop("_awaiting_confirm", None)
        MemoryManager.delete_draft_state(phone_number, "grievance")
        return {"draft_grievance": draft, "missing_fields": ALL_FIELDS}

    # 1. START NEW / ANOTHER COMPLAINT:
    # Handles: "file another complaint", "new complaint", "start over", "register another complaint"
    # Works ANYTIME — even if currently awaiting confirmation on a previous draft!
    if is_start_new_workflow(ui_lower):
        logger.info(f"[grievance.intent_and_ui] Citizen requested fresh complaint: '{user_msg}'. Discarding old draft.")
        draft = {f: None for f in ALL_FIELDS}
        draft.pop("_awaiting_confirm", None)
        MemoryManager.delete_draft_state(phone_number, "grievance")
        return {"draft_grievance": draft, "missing_fields": ALL_FIELDS}

    if is_explicit_cancel(ui_lower) and not draft.get("_awaiting_confirm"):
        draft = {f: None for f in ALL_FIELDS}
        MemoryManager.delete_draft_state(phone_number, "grievance")
        resp  = (
            "**Draft Reset**\n\n"
            "Your complaint draft has been cleared. Please describe the issue "
            "you are facing and I will guide you through the filing process."
        )
        MemoryManager.save_long_term_interaction(phone_number=phone_number, role="user",      content=user_msg)
        MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)
        return {"messages": [AIMessage(content=resp)], "draft_grievance": draft, "missing_fields": ALL_FIELDS}

    # Explicit continue signal
    if ui_lower == "continue" or "continue application" in ui_lower or "continue my complaint" in ui_lower:
        return {"draft_grievance": draft}

    # Check for direct complaint ID match (e.g. PG-PGR-2026-07-23-000339 or PGR-000339)
    pgr_id_match = re.search(r'(PG-PGR-[\w-]+|PGR-[\w-]+)', user_msg, re.IGNORECASE)
    complaint_id_filter = pgr_id_match.group(1) if pgr_id_match else None

    # Past complaint status check, Complaint ID search, or affirmative response
    past_kw = [
        "my complaint", "my complaints", "my grievance", "my grievances",
        "track", "ticket", "tickets", "status of complaint", "complaint status",
        "grievance status", "complaint history", "meri shikayat", "view complaint",
        "view complaints", "show complaint", "show complaints", "latest complaints",
        "recent complaints", "previous complaints", "past complaints", "list complaints"
    ]
    
    is_past_request = bool(complaint_id_filter) or any(w in ui_lower for w in past_kw) or (
        ("complaint" in ui_lower or "grievance" in ui_lower or "shikayat" in ui_lower) and
        any(w in ui_lower for w in ["show", "view", "track", "status", "list", "latest", "recent", "history", "previous", "past", "check", "my", "get"])
    )
    if not is_past_request and len(messages) >= 2 and isinstance(messages[-2], AIMessage):
        last_ai_lower = messages[-2].content.lower()
        if "viewing your complaints" in last_ai_lower or "view your complaints" in last_ai_lower or "complaints" in last_ai_lower:
            if ui_lower in ["yes", "yeah", "yup", "sure", "ok", "okay", "show", "view", "proceed"]:
                is_past_request = True

    if is_past_request:
        raw_list = pgr_search_complaints_raw(phone_number, complaint_id=complaint_id_filter)
        if raw_list and len(raw_list) > 0:
            raw_json = json.dumps(raw_list)
            header_text = f"Here are the details for Complaint ID `{complaint_id_filter}`:" if complaint_id_filter else "Here are your recent registered complaints:"
            resp = f"{header_text}\n<ui-complaint-history data='{raw_json}' />"
            MemoryManager.save_long_term_interaction(phone_number=phone_number, role="user", content=user_msg)
            MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)
            return {
                "messages": [AIMessage(content=resp)],
                "draft_grievance": draft,
                "missing_fields": [],
                "input_type": "complaint_history",
                "options": raw_list
            }
        else:
            resp = f"No complaint found matching ID **{complaint_id_filter}**." if complaint_id_filter else "No previous complaints found for your account."
            MemoryManager.save_long_term_interaction(phone_number=phone_number, role="user", content=user_msg)
            MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)
            return {"messages": [AIMessage(content=resp)], "draft_grievance": draft, "missing_fields": [], "input_type": "text", "options": []}

    # Pass-through
    return {"draft_grievance": draft}


# Decides whether to stop here or send the chat to the AI for data extraction
def intent_router(state: GrievanceState) -> str:
    messages = state.get("messages", [])
    if messages and isinstance(messages[-1], AIMessage):
        return END
    return "extraction"


# Uses AI and deterministic matching to extract missing complaint details
def extraction_node(state: GrievanceState):
    draft = state.get("draft_grievance") or {}
    if not isinstance(draft, dict):
        draft = {}
    messages = state.get("messages", [])
    if not messages:
        return {"draft_grievance": draft}

    user_msg = messages[-1].content.strip()
    ui_lower = user_msg.lower()
    phone_number = state.get("phone_number", "default")
    last_ai  = ""
    if len(messages) >= 2 and isinstance(messages[-2], AIMessage):
        last_ai = messages[-2].content

    # Detect yes/no at confirmation step using word boundaries
    # Prevents words like "nothing","normal" from matching "no"
    # Detect yes/no at confirmation step
    if draft.get("_awaiting_confirm"):
        clean_msg = ui_lower.strip()

        # If citizen says "file another complaint" or "start new", clear and start fresh
        if is_start_new_workflow(clean_msg):
            logger.info(f"[grievance.extraction_node] Citizen requested new complaint at confirmation: '{clean_msg}'. Discarding draft.")
            draft = {f: None for f in ALL_FIELDS}
            draft.pop("_awaiting_confirm", None)
            MemoryManager.delete_draft_state(phone_number, "grievance")
            return {"draft_grievance": draft, "missing_fields": ALL_FIELDS}

        # 1. Explicit cancellation anywhere in message:
        # Matches: "cancel", "leave it", "rehne do", "don't want to file", "do not want to continue", "nahi karna"
        has_cancel = is_explicit_cancel(clean_msg) or is_bare_negation(clean_msg) or clean_msg in ("2", "option 2")
        has_confirm = is_affirmative(clean_msg)

        if has_cancel and not has_confirm:
            draft["_cancelled"] = True
            draft["_awaiting_confirm"] = False
            return {"draft_grievance": draft}
        if has_confirm and not has_cancel:
            draft["_confirmed"] = True
            draft["_awaiting_confirm"] = False
            return {"draft_grievance": draft}

        # If user typed something else (e.g. "there is no streetlight in my area"),
        # do NOT cancel! Keep awaiting_confirm True so the prompt repeats safely.
        return {"draft_grievance": draft}

    # Meta / navigation / draft resume commands should NEVER be extracted as form field values
    meta_commands = [
        "continue", "resume", "continue application", "continue my application",
        "continue complaint", "continue my complaint", "resume draft",
        "resume my application", "open draft", "show drafts", "show my drafts",
        "draft", "drafts", "my drafts", "view drafts", "list drafts", "my draft",
        "start over", "cancel draft", "delete draft", "clear draft",
        "aage badho", "chalu karo", "continue please", "proceed", "next", "skip"
    ]
    ui_clean = re.sub(r'[^\w\s]', '', user_msg).strip().lower()
    is_meta_cmd = any(ui_clean == w or ui_clean.startswith(w + " ") for w in meta_commands)  

    # Navigation / resume commands should NEVER be extracted as form values or trigger validation errors
    if is_meta_cmd:
        logger.info(f"[grievance extract_node] User message '{user_msg}' is a navigation/resume command; skipping extraction.")
        draft.pop("_field_errors", None)
        draft.pop("_failed_fields", None)
        return {"draft_grievance": draft}

    # Detect if user's message is a question/FAQ query rather than a form answer
    question_triggers = ["?", "what is", "what does", "explain", "meaning", "kya hai", "kaise", "kyun", "tell me about"]
    is_question = any(q in ui_lower for q in question_triggers)
    if is_question:
        logger.info(f"[grievance extract_node] User asked a question ('{user_msg}'), skipping field extraction.")
        return {"draft_grievance": draft}

    # Keep extraction active only while a chat-extractable field is missing.
    missing = [f for f in EXTRACTABLE_FIELDS if not draft.get(f)]
    if not missing:
        return {"draft_grievance": draft}

    next_step = get_next_step(draft)
    curr_field = next_step.get("id")

    def _validate_and_set(field_id: str, raw_value: Any, valid_opts: list = None) -> tuple:
        """
        Validate a field value using validator.py and update the draft state.

        Args:
            field_id: Form field identifier being validated.
            raw_value: Extracted or user-provided input value.
            valid_opts: Optional list of acceptable options for option-based fields.

        Returns:
            tuple: (is_valid: bool, err_msg: Optional[str], clean_val: Any)

        Side effects:
            Updates draft[field_id], _failed_fields, _field_errors, and _raw_locality.
        """
        field_def = next((f for f in FORM_FIELDS if f["id"] == field_id), {})
        is_valid, err_msg, clean_val = validate_field(field_def, raw_value, draft=draft, valid_options=valid_opts)
        if is_valid:
            draft[field_id] = clean_val
            if field_id in draft.get("_failed_fields", []):
                draft["_failed_fields"].remove(field_id)
            draft.get("_field_errors", {}).pop(field_id, None)

            # Sync description location with chosen MDMS locality using word boundaries
            if field_id == "locality" and draft.get("description"):
                old_loc = draft.get("_raw_locality")
                if old_loc and str(old_loc).lower() != str(clean_val).lower():
                    pattern = re.compile(r'\b' + re.escape(str(old_loc)) + r'\b', re.IGNORECASE)
                    if pattern.search(draft["description"]):
                        draft["description"] = pattern.sub(clean_val, draft["description"], count=1)
                draft["_raw_locality"] = clean_val
        else:
            if not draft.get(field_id):
                draft[field_id] = None
            if field_id == "locality" and raw_value:
                draft["_raw_locality"] = str(raw_value).strip()
            draft.setdefault("_failed_fields", [])
            if field_id not in draft["_failed_fields"]:
                draft["_failed_fields"].append(field_id)
            draft.setdefault("_field_errors", {})[field_id] = err_msg
        return is_valid, err_msg, clean_val

    # Deterministic extraction based on the active step being answered
    cleaned_input = re.sub(r'^\d+[\.\)]\s*', '', user_msg).strip() # Strip leading numbering e.g. "1. PropertyTax" -> "PropertyTax"
    # Use centralized lexicon to ensure consistent exclusion across all fields
    EXCLUDED_WORDS = EXCLUDED_FIELD_WORDS
    
    if curr_field == "category":
        cats = draft.get("_category_options") or _pgr_categories()
        draft["_category_options"] = cats
        cat_keys = list(cats.keys())
        
        # Check option index matching (e.g. "1" or "Option 1")
        idx_match = re.match(r'^(?:option\s*)?(\d+)$', ui_lower)
        if idx_match:
            idx = int(idx_match.group(1)) - 1
            if 0 <= idx < len(cat_keys):
                chosen_cat = cat_keys[idx]
                is_valid, err_msg, clean_val = _validate_and_set("category", chosen_cat, cat_keys)
                if is_valid:
                    draft["_sub_options"] = cats[clean_val]
                    logger.info(f"[grievance extract] category resolved by index {idx+1}: {clean_val}")
        
        if not draft.get("category"):
            norm_input = re.sub(r'[\s_]+', '', cleaned_input.lower())
            matched_cat = None
            # Guard: Refusal when asked for Category
            if norm_input in EXCLUDED_WORDS:
                draft.setdefault("_failed_fields", [])
                if "category" not in draft["_failed_fields"]:
                    draft["_failed_fields"].append("category")
                draft.setdefault("_field_errors", {})["category"] = (
                    "**Grievance Category is required** to direct your complaint to the correct municipal department. "
                    "Please select a category from the options below, or type **'cancel'** to exit."
                )

            # Guard: Refusal words must NEVER match as a category
            if norm_input not in EXCLUDED_WORDS:
                # 1. Exact match
                matched_cat = next((k for k in cat_keys if re.sub(r'[\s_]+', '', k.lower()) == norm_input), None)
                # 2. Substring match only if input has 3+ characters (prevents short words like "no" matching)
                if not matched_cat and len(norm_input) >= 3:
                    matched_cat = next(
                        (k for k in cat_keys if norm_input in re.sub(r'[\s_]+', '', k.lower()) or (len(k) >= 3 and re.sub(r'[\s_]+', '', k.lower()) in norm_input)),
                                None
                    )
            
            if matched_cat:
                is_valid, err_msg, clean_val = _validate_and_set("category", matched_cat, cat_keys)
                if is_valid:
                    draft["_sub_options"] = cats[clean_val]
                    logger.info(f"[grievance extract] category matched: {clean_val}")

    elif curr_field == "sub_category":
        cat_selected = draft.get("category", "")
        cats = draft.get("_category_options") or _pgr_categories()
        sub_ops = draft.get("_sub_options") or cats.get(cat_selected, [])
        draft["_sub_options"] = sub_ops
        sub_names = [s["name"] for s in sub_ops]

        # Check option index matching
        idx_match = re.match(r'^(?:option\s*)?(\d+)$', ui_lower)
        if idx_match and sub_ops:
            idx = int(idx_match.group(1)) - 1
            if 0 <= idx < len(sub_ops):
                chosen_sub = sub_ops[idx]
                is_valid, err_msg, clean_val = _validate_and_set("sub_category", chosen_sub["name"], sub_names)
                if is_valid:
                    draft["category_code"] = chosen_sub["code"]
                    logger.info(f"[grievance extract] sub_category resolved by index {idx+1}: {clean_val}")

        if not draft.get("sub_category") and sub_ops:
            norm_input = re.sub(r'[\s_]+', '', cleaned_input.lower())
            matched = None
            # Guard: Refusal when asked for Sub-Category / Complaint Type
            if norm_input in EXCLUDED_WORDS:
                draft.setdefault("_failed_fields", [])
                if "sub_category" not in draft["_failed_fields"]:
                    draft["_failed_fields"].append("sub_category")
                draft.setdefault("_field_errors", {})["sub_category"] = (
                    "**Complaint Type is required** so the right field crew and equipment can be dispatched. "
                    "Please select an option from the list, or type **'cancel'** to exit."
                )
     
            # Guard: Refusal words must NEVER match as a sub-category
            if norm_input not in EXCLUDED_WORDS:
            # 1. Exact match
                matched = next((s for s in sub_ops if re.sub(r'[\s_]+', '', s["name"].lower()) == norm_input), None)
                # 2. Substring match only if input has 3+ characters
                if not matched and len(norm_input) >= 3:
                    matched = next(
                    (s for s in sub_ops if norm_input in re.sub(r'[\s_]+', '', s["name"].lower()) or (len(s["name"]) >= 3 and re.sub(r'[\s_]+', '', s["name"].lower()) in norm_input)),
                            None
                    )
                if matched:
                    is_valid, err_msg, clean_val = _validate_and_set("sub_category", matched["name"], sub_names)
                    if is_valid:
                        draft["category_code"] = matched["code"]
                        logger.info(f"[grievance extract] sub_category matched: {clean_val}")


    elif curr_field == "locality":
        loc_ops = draft.get("_locality_options") or _pgr_localities()
        draft["_locality_options"] = loc_ops
        loc_names = [l["name"] for l in loc_ops]

        idx_match = re.match(r'^(?:option\s*)?(\d+)$', ui_lower)
        if idx_match and loc_ops:
            idx = int(idx_match.group(1)) - 1
            if 0 <= idx < len(loc_ops):
                chosen_loc = loc_ops[idx]
                is_valid, err_msg, clean_val = _validate_and_set("locality", chosen_loc["name"], loc_names)
                if is_valid:
                    draft["locality_code"] = chosen_loc["code"]
                    logger.info(f"[grievance extract] locality resolved by index {idx+1}: {clean_val}")

        # Guard: Refusal words must NEVER match as an area or locality

        if not draft.get("locality") and loc_ops:
            norm_input = re.sub(r'[\s_]+', '', cleaned_input.lower())
            matched = None
            # Guard: Refusal when asked for Area / Locality
            if norm_input in EXCLUDED_WORDS:
                draft.setdefault("_failed_fields", [])
                if "locality" not in draft["_failed_fields"]:
                    draft["_failed_fields"].append("locality")
                draft.setdefault("_field_errors", {})["locality"] = (
                    "**Area / Locality is required** so our municipal officers can locate and resolve the issue. "
                    "Please select your locality from the list, or type **'cancel'** to exit."
                )
            if norm_input not in EXCLUDED_WORDS:
                matched = next((l for l in loc_ops if re.sub(r'[\s_]+', '', l["name"].lower()) == norm_input), None)
            # 2. Substring match only if input has 3+ characters (prevents "no" matching "Gali No.")
            if not matched and len(norm_input) >= 3:
                matched = next((l for l in loc_ops if norm_input in re.sub(r'[\s_]+', '', l["name"].lower()) or (len(l["name"]) >= 3 and re.sub(r'[\s_]+', '', l["name"].lower()) in norm_input)), None)
            if matched:
                is_valid, err_msg, clean_val = _validate_and_set("locality", matched["name"], loc_names)
                if is_valid:
                    draft["locality_code"] = matched["code"]
                    logger.info(f"[grievance extract] locality matched: {clean_val}")

    # Fallback to LLM multi-field extraction if any missing fields remain and LLM is available
    # Recheck after deterministic matching; only unresolved extractable fields go to the LLM.
    remaining_missing = [f for f in EXTRACTABLE_FIELDS if not draft.get(f)]
    if remaining_missing and llm:
        schema_hints = {}
        for f in remaining_missing:
            if f == "category":
                cats = draft.get("_category_options") or _pgr_categories()
                opts = list(cats.keys())
                schema_hints[f] = f"MUST MATCH one of: {opts}" if opts else "Grievance Category"
            elif f == "sub_category":
                sub_ops = [s["name"] for s in (draft.get("_sub_options") or [])]
                schema_hints[f] = f"MUST MATCH one of: {sub_ops}" if sub_ops else "Specific complaint type"
            elif f == "locality":
                schema_hints[f] = "Extract whatever area or locality the user mentioned as text."
            elif f == "description":
                schema_hints[f] = "Detailed description of the issue provided by the citizen (e.g. 'i want a refund', 'dirty water from tap', 'pothole on road')"

        collected_str = json.dumps({k: v for k, v in draft.items() if v and not k.startswith("_")})
        extract_prompt = build_grievance_extract_prompt(
            remaining_missing=remaining_missing,
            schema_hints=schema_hints,
            collected_str=collected_str,
            last_ai=last_ai,
            user_msg=user_msg
        )

        try:
            ext = llm.invoke([SystemMessage(content=extract_prompt)])
            m = re.search(r'\{.*\}', ext.content.strip(), re.DOTALL)
            if m:
                extracted = json.loads(m.group(0))
                for field in remaining_missing:
                    val = extracted.get(field)
                    if val is not None and str(val).lower() not in ("null", "none", ""):
                        val_str = str(val).strip()
                        if val_str.lower() in meta_commands:
                            continue
                        if field == "description":
                            is_valid, err_msg, clean_val = _validate_and_set("description", val_str)
                            if is_valid:
                                logger.info(f"[grievance LLM extract] description = {clean_val}")
                        elif field == "category" and not draft.get("category"):
                            cats = draft.get("_category_options") or _pgr_categories()
                            is_valid, err_msg, clean_val = _validate_and_set("category", val_str, list(cats.keys()))
                            if is_valid:
                                draft["_sub_options"] = cats[clean_val]
                        elif field == "sub_category" and not draft.get("sub_category"):
                            sub_ops = draft.get("_sub_options") or []
                            is_valid, err_msg, clean_val = _validate_and_set("sub_category", val_str, [s["name"] for s in sub_ops])
                            if is_valid:
                                matched = next((s for s in sub_ops if s["name"] == clean_val), None)
                                if matched:
                                    draft["category_code"] = matched["code"]
                        elif field == "locality" and not draft.get("locality"):
                            loc_ops = draft.get("_locality_options") or _pgr_localities()
                            is_valid, err_msg, clean_val = _validate_and_set("locality", val_str, [l["name"] for l in loc_ops])
                            if is_valid:
                                matched = next((l for l in loc_ops if l["name"] == clean_val), None)
                                if matched:
                                    draft["locality_code"] = matched["code"]
        except Exception as e:
            logger.error(f"[grievance] LLM extraction error: {e}")
    # CONTEXT-AWARE FIELD WARNING GUARD:
    # Why this is required:
    # In multi-turn chat, 'last_ai' might contain an out-of-domain reply, greeting, or FAQ answer
    # (e.g. "I can only help with UPYOG services"). If the citizen then says "register complaint",
    # that is a workflow trigger — NOT an invalid category name.
    # Therefore, we ONLY warn the citizen if the bot ACTUALLY asked for that specific field
    # in the previous turn and the citizen's response failed to match.
    field_prompt_signatures = {
        "category": ["category"],
        "sub_category": ["complaint type", "sub category", "subcategory"],
        "locality": ["locality", "area", "mohalla"]
    }
    
    bot_actually_asked = any(
        kw in last_ai.lower() for kw in field_prompt_signatures.get(curr_field, [])
    )

    if bot_actually_asked and curr_field and not draft.get(curr_field) and curr_field not in draft.get("_field_errors", {}):
        if not is_start_new_workflow(user_msg) and not is_meta_cmd and norm_input not in EXCLUDED_WORDS:
            if curr_field == "sub_category":
                # Wipe mismatched description so it doesn't linger and contradict the chosen complaint type
                draft["description"] = None
                draft.setdefault("_field_errors", {})["sub_category"] = (
                    f"**'{user_msg}' does not match any complaint type for {draft.get('category')}.** "
                    "Please select an option from the list below, or type **'cancel'** to exit."
                )
            elif curr_field == "category":
                draft["description"] = None
                draft.setdefault("_field_errors", {})["category"] = (
                    f"**'{user_msg}' was not recognized as a supported category.** "
                    "Please choose from the options below, or type **'cancel'** to exit."
                )
            elif curr_field == "locality":
                draft.setdefault("_field_errors", {})["locality"] = (
                    f"**'{user_msg}' was not found in our municipal locality database.** "
                    "Please select your locality from the list below so officers can locate the issue."
                )

    # Auto-Save to Qdrant Disk Store on every turn if fields are present
    phone_number = state.get("phone_number", "default")
    if draft and any(v for k, v in draft.items() if not k.startswith("_") and v is not None) and phone_number and phone_number != "default":
        MemoryManager.save_draft_state(phone_number, "grievance", draft)

    return {"draft_grievance": draft}


# Asks the user the next missing question and shows dropdown options if applicable
def ask_next_node(state: GrievanceState):
    phone_number = state.get("phone_number", "default")
    draft = state.get("draft_grievance") or {}
    if not isinstance(draft, dict):
        draft = {}
    messages = state.get("messages", [])
    user_msg = messages[-1].content if messages else ""

    next_step = get_next_step(draft)
    if not next_step:
        return {}

    field_id    = next_step["id"]
    field_label = next_step.get("label", field_id)
    field_type  = next_step.get("type", "text")
    options: list = []

    if field_id == "category":
        cats = draft.get("_category_options") or _pgr_categories()
        draft["_category_options"] = cats
        options = list(cats.keys())

    elif field_id == "sub_category":
        cat_selected = draft.get("category", "")
        cats         = draft.get("_category_options") or _pgr_categories()
        sub_ops      = cats.get(cat_selected, [])
        draft["_sub_options"] = sub_ops
        options = [s["name"] for s in sub_ops]

    elif field_id == "locality":
        locs = draft.get("_locality_options") or _pgr_localities()
        draft["_locality_options"] = locs
        options = [l["name"] for l in locs[:25]]

    text = f"Please provide the {field_label}."
    if llm:
        is_hindi = any('ऀ' <= c <= 'ॿ' for c in user_msg)
        lang_rule = "Respond in Hindi." if is_hindi else "Respond in English. Do NOT output any Devanagari or Hindi text."
        ctx = build_grievance_question_prompt(
            draft=draft,
            field_label=field_label,
            field_id=field_id,
            options=options,
            user_msg=user_msg,
            lang_rule=lang_rule
        )
        try:
            conv = llm.invoke([SystemMessage(content=ctx)])
            text = conv.content.strip()
        except Exception:
            pass

    val_err = draft.get("_field_errors", {}).pop(field_id, None)
    if val_err:
        text = f"⚠️ {val_err}\n\n{text}"

    if field_type == "dropdown" and options:
        opts_str = ", ".join(f"'{o}'" for o in options)
        resp = f"{text}\n<ui-dropdown options=[{opts_str}] />"
    else:
        resp = text

    if user_msg and user_msg not in ["continue", "hello", "[CANCEL_DRAFT]"]:
        MemoryManager.save_long_term_interaction(phone_number=phone_number, role="user", content=user_msg)
    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)

    logger.info(f"[grievance.ask_next_node] Asked citizen for '{field_id}'. Missing fields left: {[f for f in ALL_FIELDS if not draft.get(f)]}")
    return {
        "messages":        [AIMessage(content=resp)],
        "draft_grievance": draft,
        "missing_fields":  [f for f in ALL_FIELDS if not draft.get(f)],
        "input_type":      "text",
        "options":         []
    }


# Shows a final summary receipt of the complaint details and asks the user to confirm submission
def confirm_node(state: GrievanceState):
    phone_number = state.get("phone_number", "default")
    draft        = dict(state.get("draft_grievance", {}))
    messages     = state.get("messages", [])
    user_msg     = messages[-1].content if messages else ""
    logger.info(f"[grievance.confirm_node] Prompting confirmation for phone={phone_number}")

    # Ensure description is meaningful
    desc = str(draft.get("description", "")).strip()
    clean_loc = draft.get("locality")
    if not desc or len(desc) < 3:
        sub = draft.get('sub_category') or draft.get('category') or 'Complaint'
        loc = clean_loc or 'the specified locality'
        draft['description'] = f"{sub} issue reported at {loc}"

    summary = (
        "**Review Your Complaint Details:**\n\n"
        f"- **Category:** {draft.get('category', 'N/A')}\n"
        f"- **Complaint Type:** {draft.get('sub_category', 'N/A')}\n"
        f"- **Description:** {draft.get('description')}\n"
        f"- **Locality:** {draft.get('locality', 'N/A')}\n\n"
        "Please reply **Yes** to submit or **No** to cancel.\n"
        "<ui-button options=['Yes', 'No'] />"
    )
    draft["_awaiting_confirm"] = True

    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="user", content=user_msg)
    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=summary)

    return {"messages": [AIMessage(content=summary)], "draft_grievance": draft}


# Submits the finalized complaint to the UPYOG server to register the grievance and get an ID
def create_complaint_node(state: GrievanceState):
    phone_number = state.get("phone_number", "default")
    draft        = dict(state.get("draft_grievance", {}))
    logger.info(f"[grievance.create_complaint_node] Submitting complaint for phone={phone_number}")

    payload = {
        "phone_number":  phone_number,
        "category_code": draft.get("category_code", ""),
        "sub_category":  draft.get("sub_category",  ""),
        "description":   draft.get("description",   ""),
        "locality":      draft.get("locality",       ""),
        "locality_code": draft.get("locality_code",  ""),
    }

    raw_resp = pgr_create_complaint(json.dumps(payload))
    resp = f"{raw_resp}\n\nIs there anything else I can help you with?"
    logger.info(f"[grievance.create_complaint_node] Result: {raw_resp}")
    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)

    reset_draft = {f: None for f in ALL_FIELDS}
    MemoryManager.delete_draft_state(phone_number, "grievance")
    return {"messages": [AIMessage(content=resp)], "draft_grievance": reset_draft, "missing_fields": ALL_FIELDS, "input_type": "text", "options": []}


# Cancels the current complaint draft and clears the memory
def cancel_node(state: GrievanceState):
    phone_number = state.get("phone_number", "default")
    messages     = state.get("messages", [])
    user_msg     = messages[-1].content if messages else ""
    logger.info(f"[grievance.cancel_node] Cancelling complaint draft for phone={phone_number}")

    resp = (
        "**Complaint Withdrawn**\n\n"
        "Your complaint has been cancelled. No submission was made to the portal.\n\n"
        "If you wish to file a new complaint, please describe the issue and I will assist you."
    )
    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="user", content=user_msg)
    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)
    

    # Clean up draft and ensure all control flags are explicitly set to False/None
    # Why this is required: Prevents leaving a '_cancelled: True' state in the checkpointer
    reset_draft = {f: None for f in ALL_FIELDS}
    reset_draft["_cancelled"]=False
    reset_draft["_confirmed"]=False
    reset_draft["_awaiting_confirm"]=False
    MemoryManager.delete_draft_state(phone_number, "grievance")
    return {"messages": [AIMessage(content=resp)], "draft_grievance": reset_draft, "missing_fields": ALL_FIELDS}


# The main traffic controller that routes the flow to the correct next step based on what's missing
# def router(state: GrievanceState) -> str:
#     draft      = state.get("draft_grievance") or {}
#     all_filled = all(draft.get(f) for f in ALL_FIELDS)

#     if draft.get("_cancelled"):
#         logger.info("[grievance.router] Route -> cancel")
#         return "cancel"
#     if draft.get("_confirmed"):
#         logger.info("[grievance.router] Route -> create_complaint")
#         return "create_complaint"
#     if not all_filled:
#         logger.info(f"[grievance.router] Route -> ask_next (missing fields: {[f for f in ALL_FIELDS if not draft.get(f)]})")
#         return "ask_next"
#     logger.info("[grievance.router] Route -> confirm")
#     return "confirm"
# Handles configuration or critical runtime errors gracefully
def error_node(state: GrievanceState):
    """
    Why this is required:
    If form fields are ever missing at runtime, this node informs the citizen
    honestly that the service is temporarily unavailable, rather than falsely
    saying 'Complaint Withdrawn' or displaying an empty confirmation screen.
    """
    phone_number = state.get("phone_number", "default")
    logger.error(f"[grievance.error_node] Grievance workflow halted due to missing ALL_FIELDS for phone={phone_number}")

    resp = (
        "**Service Temporarily Unavailable**\n\n"
        "We are unable to process your complaint registration at this time due to a technical configuration issue. "
        "No submission was made to the municipal portal.\n\n"
        "Please try again shortly or contact the municipal helpdesk."
    )
    MemoryManager.save_long_term_interaction(phone_number=phone_number, role="assistant", content=resp)

    # Clean up draft so the user is not trapped in a broken state
    reset_draft = {f: None for f in ALL_FIELDS} if ALL_FIELDS else {}
    MemoryManager.delete_draft_state(phone_number, "grievance")
    return {
        "messages": [AIMessage(content=resp)],
        "draft_grievance": reset_draft,
        "missing_fields": [],
        "input_type": "text",
        "options": []
    }


# The main traffic controller that routes the flow to the correct next step based on what's missing
def router(state: GrievanceState) -> str:
    draft = state.get("draft_grievance") or {}

    # DEFENSIVE GUARD: If form fields are empty, route to error node.
    # Why: In Python, all([]) is True! Without this check, empty fields cause an immediate jump to confirmation.
    if not ALL_FIELDS:
        logger.error("[grievance.router] Route -> error: ALL_FIELDS is empty. Workflow misconfigured.")
        return "error"

    if draft.get("_cancelled"):
        logger.info("[grievance.router] Route -> cancel")
        return "cancel"
    if draft.get("_confirmed"):
        logger.info("[grievance.router] Route -> create_complaint")
        return "create_complaint"

    all_filled = all(draft.get(f) for f in ALL_FIELDS)
    if not all_filled:
        logger.info(f"[grievance.router] Route -> ask_next (missing fields: {[f for f in ALL_FIELDS if not draft.get(f)]})")
        return "ask_next"

    logger.info("[grievance.router] Route -> confirm")
    return "confirm"


# ==========================================
# GRAPH ASSEMBLY
# ==========================================

_wf = StateGraph(GrievanceState)

_wf.add_node("intent_and_ui",    intent_and_ui_node)
_wf.add_node("extraction",       extraction_node)
_wf.add_node("ask_next",         ask_next_node)
_wf.add_node("confirm",          confirm_node)
_wf.add_node("create_complaint", create_complaint_node)
_wf.add_node("cancel",           cancel_node)
_wf.add_node("error", error_node)

_wf.add_edge(START, "intent_and_ui")
_wf.add_conditional_edges(
    "intent_and_ui",
    intent_router,
    {"extraction": "extraction", END: END}
)
_wf.add_conditional_edges(
    "extraction",
    router,
    {
        "ask_next":         "ask_next",
        "confirm":          "confirm",
        "create_complaint": "create_complaint",
        "cancel":           "cancel",
        "error":            "error",
    }
)
_wf.add_edge("ask_next",         END)
_wf.add_edge("confirm",          END)
_wf.add_edge("create_complaint", END)
_wf.add_edge("cancel",           END)
_wf.add_edge("error",            END)

# Export -- auto-discovered by load_plugins() via the <module>_graph naming convention
grievance_graph = _wf.compile(checkpointer=shared_memory)
