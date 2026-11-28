"""
Central validation service for UPYOG workflows.

Provides validation, normalization, and informative error reporting for user-submitted
form inputs across conversational flows (e.g. Advertisement Booking and Grievance).
"""

import base64
import difflib
import os
import re
from datetime import datetime
from typing import Any, Optional, Tuple

try:
    from zoneinfo import ZoneInfo
    IST = ZoneInfo("Asia/Kolkata")
except Exception:
    from datetime import timezone, timedelta
    IST = timezone(timedelta(hours=5, minutes=30))


ALLOWED_DOC_EXTENSIONS = {".jpg", ".jpeg", ".png", ".pdf"}
MAX_DOC_SIZE_BYTES = 2 * 1024 * 1024  # 2 MB
_UUID_RE = re.compile(r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
_PINCODE_RE = re.compile(r"\b[1-9][0-9]{5}\b")
_WORD_RE = re.compile(r"[a-zA-Z0-9]+")
_DIGIT_RE = re.compile(r"\d")
_LETTER_RE = re.compile(r"[a-zA-Z\u0900-\u097F]")
_NORM_RE = re.compile(r"[\s_]+")


def is_empty(value: Any) -> bool:
    """Check whether a value is empty or represents a null/none placeholder."""
    return value is None or str(value).strip().lower() in ("", "null", "none")


def normalize(value: Any) -> str:
    """Normalize string by lowercasing and stripping whitespace and underscores."""
    return _NORM_RE.sub("", str(value).lower())


def _format_val(val: Any, max_chars: int = 30) -> str:
    """Formats a user input value cleanly inside quotes for error messages."""
    s = str(val).strip()
    return f"'{s[:max_chars - 3]}...'" if len(s) > max_chars else f"'{s}'"


def _parse_date(raw: Any):
    """Parses YYYY-MM-DD strictly, falling back to dateutil (dayfirst=True, IST default)."""
    text = str(raw).strip()
    try:
        return datetime.strptime(text, "%Y-%m-%d").date()
    except ValueError:
        from dateutil import parser as dt_parser
        default_dt = datetime.now(IST).replace(tzinfo=None)
        return dt_parser.parse(text, default=default_dt, dayfirst=True).date()


# ==============================================================================
# DECLARATIVE DIAGNOSTIC HELPERS (Replaces chained if-elif-else ladders)
# ==============================================================================

def _diagnose_email_error(value: str) -> str:
    """Declarative diagnostic rules for invalid email addresses."""
    rules = (
        ("@" not in value, "missing '@' symbol"),
        (value.startswith("@"), "missing username before '@'"),
        (value.endswith("@") or "@." in value, "missing domain name after '@'"),
        ("." not in value.split("@")[-1], "missing domain extension like .com or .in"),
        (" " in value, "email address cannot contain spaces"),
    )
    return next((msg for cond, msg in rules if cond), "improper email format")


def _diagnose_mobile_error(mobile: str) -> str:
    """Declarative diagnostic rules for invalid Indian mobile numbers."""
    rules = (
        (not mobile.isdigit(), "contains non-numeric characters"),
        (len(mobile) < 10, f"only {len(mobile)} digits entered, exactly 10 digits required"),
        (len(mobile) > 10, f"{len(mobile)} digits entered, exactly 10 digits required"),
        (not mobile.startswith(('6', '7', '8', '9')), "must start with 6, 7, 8, or 9"),
    )
    return next((msg for cond, msg in rules if cond), "invalid mobile number format")


def _diagnose_name_error(name_clean: str, clean_v: str, pattern: str) -> Optional[str]:
    """Declarative diagnostic rules for applicant names."""
    rules = (
        (len(name_clean) < 2, f"{clean_v} is too short. Expected full name with at least 2 alphabetic characters (e.g., Rajesh Kumar)."),
        (len(name_clean) > 50, "Name exceeds 50 characters limit. Please provide a full name under 50 characters."),
        (bool(_DIGIT_RE.search(name_clean)), f"Invalid format: {clean_v} contains numbers. Names cannot contain numbers. Expected full name using only letters and spaces (e.g., Rajesh Kumar)."),
        (not bool(_LETTER_RE.search(name_clean)), f"{clean_v} is invalid (must contain alphabetic letters). Expected full name (e.g., Rajesh Kumar)."),
        (not bool(re.fullmatch(pattern, name_clean)), f"{clean_v} contains invalid special characters. Expected full name using only letters and spaces (e.g., Rajesh Kumar)."),
    )
    return next((msg for cond, msg in rules if cond), None)


# ==============================================================================
# CORE VALIDATORS
# ==============================================================================

def validate_date(field_info: Any, value: Any, label: Optional[str] = None, draft: Optional[dict] = None, **kwargs) -> Tuple[bool, Optional[str], Optional[str]]:
    """Validate date format and date-relationship constraints against config rules."""
    if isinstance(field_info, dict):
        field_def = field_info
        field_id = field_def.get("id", "")
        label = field_def.get("label", label or field_id)
        validation_cfg = field_def.get("validation", {})
    else:
        field_id = str(field_info)
        label = label or field_id
        validation_cfg = {}

    clean_v = _format_val(value)
    try:
        parsed = _parse_date(value)
    except Exception:
        return False, f"{clean_v} is not a valid date. Expected format: YYYY-MM-DD.", None

    today_ist = datetime.now(IST).date()

    # after_today check (applies if configured or if field_id is start_date)
    if (validation_cfg.get("after_today") or field_id == "start_date") and parsed <= today_ist:
        return False, f"{label} ({clean_v}) must be tomorrow or later.", None

    # on_or_after check (from config or end_date vs start_date)
    ref_field = validation_cfg.get("on_or_after") or ("start_date" if field_id == "end_date" else None)
    if ref_field and draft and draft.get(ref_field):
        ref_val = str(draft[ref_field]).strip()
        try:
            ref_date = _parse_date(ref_val)
        except Exception:
            return False, "Please correct the Start Date first.", None
        if parsed < ref_date:
            ref_label = "Start Date" if ref_field == "start_date" else ref_field
            return False, f"{label} ({clean_v}) cannot be earlier than {ref_label} ({ref_val}).", None

    return True, None, parsed.strftime("%Y-%m-%d")


def validate_document(
    file_name: str,
    file_data_base64: Optional[str] = None,
    file_size_bytes: Optional[int] = None,
    label: str = "Document"
) -> Tuple[bool, Optional[str], Optional[str]]:
    """
    Validates document format and file size.
    Accepted formats: JPG, JPEG, PNG, PDF.
    Maximum file size: Less than 2 MB (2,097,152 bytes).
    """
    if not file_name or not str(file_name).strip():
        return False, f"Please select a valid {label.lower()} to upload in JPG, PNG, or PDF format.", None

    clean_name = str(file_name).strip()
    ext = os.path.splitext(clean_name)[1].lower()

    if ext not in ALLOWED_DOC_EXTENSIONS:
        clean_ext = ext if ext else "unknown"
        return (
            False,
            f"'{clean_name}' has an unsupported file format ({clean_ext}). Supported formats are JPG, PNG, or PDF (file size must be less than 2 MB).",
            None
        )

    size_bytes = file_size_bytes
    if size_bytes is None and file_data_base64:
        try:
            raw_b64 = file_data_base64.split(",", 1)[1] if "," in file_data_base64 else file_data_base64
            size_bytes = len(base64.b64decode(raw_b64.strip()))
        except Exception:
            pass

    if size_bytes is not None and size_bytes > MAX_DOC_SIZE_BYTES:
        size_mb = size_bytes / (1024 * 1024)
        return (
            False,
            f"'{clean_name}' ({size_mb:.1f} MB) exceeds the 2 MB limit. Please upload a file smaller than 2 MB in JPG, PNG, or PDF format.",
            None
        )

    return True, None, clean_name


# ==============================================================================
# MODULAR FIELD VALIDATORS (Strategy Handlers)
# ==============================================================================

def _validate_options(field_def: dict, value: str, clean_v: str, valid_options: Optional[list] = None, **kwargs) -> Tuple[bool, Optional[str], Any]:
    """Validates choice against MDMS/dropdown options with exact, substring, and typo tolerance."""
    options = valid_options or field_def.get("options") or []
    norm_val = normalize(value)

    def _opt_str(opt):
        return str(opt.get("name") or opt.get("label") or opt.get("code") or "") if isinstance(opt, dict) else str(opt)

    # 1. Exact match
    for opt in options:
        if norm_val == normalize(_opt_str(opt)):
            return True, None, opt

    # 2. Substring match (>= 3 chars, unique match)
    if len(norm_val) >= 3:
        sub_matches = [opt for opt in options if norm_val in normalize(_opt_str(opt))]
        if len(sub_matches) == 1:
            return True, None, sub_matches[0]

    # 3. High-confidence typo match (>= 85% similarity)
    for opt in options:
        opt_str = _opt_str(opt)
        words = _WORD_RE.findall(opt_str)
        if any(len(w) >= 4 and difflib.SequenceMatcher(None, norm_val, w.lower()).ratio() >= 0.85 for w in words):
            return True, None, opt
        if difflib.SequenceMatcher(None, norm_val, normalize(opt_str)).ratio() >= 0.85:
            return True, None, opt

    field_id = field_def.get("id", "")
    label = field_def.get("label", field_id)
    is_loc = field_id in ("location", "locality") or (
        any(k in label.lower() for k in ("location", "locality"))
        and "face" not in label.lower()
        and "face" not in field_id.lower()
    )
    if is_loc:
        return False, f"The specified location {clean_v} is not a valid option. Please select a valid location from the options below.", None

    clean_label = label.replace("Advertisement ", "").replace("Complaint ", "").strip()
    return False, f"{clean_v} is not a valid {clean_label.lower()}. Please select a valid option from the options below.", None


def _validate_mobile(field_def: dict, value: str, clean_v: str, **kwargs) -> Tuple[bool, Optional[str], Any]:
    """Validates 10-digit Indian mobile numbers starting with 6-9."""
    mobile = re.sub(r"[\s\-+]", "", value)
    if mobile.startswith("91") and len(mobile) == 12:
        mobile = mobile[2:]
    elif mobile.startswith("0") and len(mobile) == 11:
        mobile = mobile[1:]

    validation_cfg = field_def.get("validation", {})
    pattern = validation_cfg.get("pattern") or r"^[6-9][0-9]{9}$"
    if not re.fullmatch(pattern, mobile):
        reason = _diagnose_mobile_error(mobile)
        return False, f"{clean_v} is not a valid mobile number ({reason}). Expected format: a 10-digit Indian mobile number starting with 6-9 (e.g., 9876543210).", None

    return True, None, mobile


def _validate_email(field_def: dict, value: str, clean_v: str, **kwargs) -> Tuple[bool, Optional[str], Any]:
    """Validates standard email address syntax with informative error explanation."""
    validation_cfg = field_def.get("validation", {})
    pattern = validation_cfg.get("pattern") or r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z]{2,}$"
    if not re.fullmatch(pattern, value):
        reason = _diagnose_email_error(value)
        return False, f"{clean_v} is not a valid email address ({reason}). Expected format: username@domain.com.", None

    user_part, domain_part = value.rsplit("@", 1) if "@" in value else (value, "")
    return True, None, f"{user_part}@{domain_part.lower()}" if domain_part else value


def _validate_name(field_def: dict, value: str, clean_v: str, **kwargs) -> Tuple[bool, Optional[str], Any]:
    """Validates full applicant names (letters and spaces only, 2-50 chars)."""
    validation_cfg = field_def.get("validation", {})
    name_clean = value.strip()
    pattern = validation_cfg.get("pattern") or r"^[a-zA-Z\u0900-\u097F .'-]+$"
    err = _diagnose_name_error(name_clean, clean_v, pattern)
    if err:
        return False, err, None
    return True, None, name_clean


def _validate_address(field_def: dict, value: str, clean_v: str, **kwargs) -> Tuple[bool, Optional[str], Any]:
    """Validates complete addresses containing house/locality and a 6-digit postal pincode."""
    if not _PINCODE_RE.search(value):
        return (
            False,
            "Address is missing a valid 6-digit postal pincode. Expected format: House/Flat No. (if available), Locality/Street name, and 6-digit pincode.",
            None
        )
    text_without_pin = _PINCODE_RE.sub("", value).strip(" ,-")
    if len(text_without_pin) < 4:
        return (
            False,
            "Address is incomplete. Please include your house/flat number (if available) and locality or street name along with the 6-digit pincode.",
            None
        )
    return True, None, value


def _validate_file(field_def: dict, value: str, **kwargs) -> Tuple[bool, Optional[str], Any]:
    """Validates uploaded document references or file attachments."""
    if value.startswith("[Uploaded Document:") or _UUID_RE.fullmatch(value):
        return True, None, value
    label = field_def.get("label", field_def.get("id", "Document"))
    is_valid_doc, doc_err, clean_name = validate_document(value, label=label)
    return (True, None, clean_name) if is_valid_doc else (False, doc_err, None)


def _validate_generic_constraints(field_def: dict, value: str, clean_v: str) -> Tuple[bool, Optional[str], Any]:
    """Fallback validation for generic pattern and length limits defined in config.yml."""
    validation_cfg = field_def.get("validation", {})
    label = field_def.get("label", field_def.get("id", ""))

    pattern = validation_cfg.get("pattern")
    if pattern and not re.fullmatch(pattern, value):
        return False, f"{clean_v} does not match the required format for {label}.", None

    min_len = validation_cfg.get("min_length")
    if min_len and len(value) < min_len:
        return False, f"{label} is too short (must be at least {min_len} characters).", None

    max_len = validation_cfg.get("max_length")
    if max_len and len(value) > max_len:
        return False, f"{label} is too long (must be at most {max_len} characters).", None

    return True, None, value


# ==============================================================================
# DISPATCH TABLE (eliminating if-else cascades)
# ==============================================================================

TYPE_DISPATCH = {
    "date": validate_date,
    "email": _validate_email,
    "mobile": _validate_mobile,
    "phone": _validate_mobile,
    "file": _validate_file,
}

ID_DISPATCH = {
    "emailId": _validate_email,
    "email": _validate_email,
    "mobileNumber": _validate_mobile,
    "phone_number": _validate_mobile,
    "applicantName": _validate_name,
    "name": _validate_name,
    "address": _validate_address,
    "applicantAddress": _validate_address,
}


def _resolve_handler(field_def: dict, has_options: bool):
    """Resolves the appropriate validation handler without if-else cascades."""
    if has_options:
        return _validate_options

    f_type = field_def.get("type", "text")
    if f_type in TYPE_DISPATCH:
        return TYPE_DISPATCH[f_type]

    f_id = field_def.get("id", "")
    if f_id in ID_DISPATCH:
        return ID_DISPATCH[f_id]

    f_id_lower = f_id.lower()
    if f_id.startswith("doc_"):
        return _validate_file
    if "address" in f_id_lower:
        return _validate_address
    if "name" in f_id_lower and f_id != "file_name":
        return _validate_name

    return None


# ==============================================================================
# ENTRY POINT
# ==============================================================================

def validate_field(field_def: dict, value: Any, draft: Optional[dict] = None, valid_options: Optional[list] = None) -> Tuple[bool, Optional[str], Any]:
    """
    Validate a single form field value against its definition and configuration.
    Uses Strategy/Dispatch pattern for concise, extensible, O(1) execution.
    """
    label = field_def.get("label", field_def.get("id", ""))
    validation_cfg = field_def.get("validation", {})

    # 1. Empty check (respects validation.required)
    if is_empty(value):
        if not validation_cfg.get("required", True):
            return True, None, None
        return False, f"Please provide {label}.", None

    value = str(value).strip()
    clean_v = _format_val(value)

    # 2. Strategy dispatch
    handler = _resolve_handler(field_def, bool(valid_options or field_def.get("options")))
    if handler:
        return handler(field_def, value, clean_v=clean_v, draft=draft, valid_options=valid_options)

    # 3. Fallback generic constraints (pattern, lengths from config.yml)
    return _validate_generic_constraints(field_def, value, clean_v)
