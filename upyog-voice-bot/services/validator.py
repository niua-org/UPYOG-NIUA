"""
Central validation service for UPYOG workflows.

Provides validation, normalization, and error reporting for user-submitted
form inputs across conversational flows (e.g. Advertisement Booking and Grievance).
"""

import difflib
import re
from datetime import datetime
try:
    from zoneinfo import ZoneInfo
    IST = ZoneInfo("Asia/Kolkata")
except Exception:
    from datetime import timezone, timedelta
    IST = timezone(timedelta(hours=5, minutes=30))


def is_empty(value):
    """
    Check whether a value is empty or represents a null/none placeholder.

    Args:
        value: Any value to check.

    Returns:
        bool: True if value is empty/null, False otherwise.
    """
    return value is None or str(value).strip().lower() in ("", "null", "none")


def normalize(value):
    """
    Normalize string by lowercasing and stripping whitespace and underscores.

    Args:
        value: String or object to normalize.

    Returns:
        str: Normalized string for case- and whitespace-insensitive comparison.
    """
    return re.sub(r"[\s_]+", "", str(value).lower())


def validate_date(field_info, value, label=None, draft=None):
    """
    Validate date format and date-relationship constraints against config rules.

    Args:
        field_info: Field definition dictionary or field_id string.
        value: Date string in 'YYYY-MM-DD' format.
        label: Optional display label for error messages (fallback if field_info is str).
        draft: Optional dictionary of currently collected form state.

    Returns:
        tuple: (is_valid: bool, error_message: Optional[str], cleaned_value: Optional[str])
    """
    if isinstance(field_info, dict):
        field_def = field_info
        field_id = field_def.get("id", "")
        label = field_def.get("label", label or field_id)
        validation_cfg = field_def.get("validation", {})
    else:
        field_id = str(field_info)
        label = label or field_id
        validation_cfg = {}

    try:
        parsed = datetime.strptime(str(value).strip(), "%Y-%m-%d").date()
    except ValueError:
        return False, f"Enter {label} in YYYY-MM-DD format.", None

    today_ist = datetime.now(IST).date()

    # after_today check (applies if configured or if field_id is start_date)
    if validation_cfg.get("after_today") or field_id == "start_date":
        if parsed <= today_ist:
            return False, f"{label} must be tomorrow or later.", None

    # on_or_after check (from config or end_date vs start_date)
    ref_field = validation_cfg.get("on_or_after") or ("start_date" if field_id == "end_date" else None)
    if ref_field and draft and draft.get(ref_field):
        try:
            ref_date = datetime.strptime(str(draft[ref_field]).strip(), "%Y-%m-%d").date()
            if parsed < ref_date:
                ref_label = "From Date" if ref_field == "start_date" else ref_field
                return False, f"{label} cannot be earlier than {ref_label}.", None
        except ValueError:
            return False, "Please correct the From Date first.", None

    return True, None, str(value).strip()


def validate_field(field_def, value, draft=None, valid_options=None):
    """
    Validate a single form field value against its definition and configuration.

    Validation Order:
      1. Empty check (respects validation.required)
      2. Options check (exact, tolerant substring, fuzzy match, supports dict options)
      3. Date check (YYYY-MM-DD format, after_today, on_or_after in IST)
      4. Mobile number check (standardizes +91/leading 0, validates pattern)
      5. Email check (domain case-normalization, validates pattern)
      6. Pattern check (regex validation from config)
      7. Length check (min_length / max_length from config)

    Args:
        field_def: Dictionary defining the field (id, label, type, validation rules).
        value: The raw input value provided by the user or extracted by LLM.
        draft: Optional dictionary representing currently collected draft state.
        valid_options: Optional list of valid options overriding field_def['options'].

    Returns:
        tuple: (is_valid: bool, error_message: Optional[str], cleaned_value: Any)
    """
    field_id = field_def.get("id", "")
    label = field_def.get("label", field_id)
    field_type = field_def.get("type", "text")
    validation_cfg = field_def.get("validation", {})
    is_req = validation_cfg.get("required", True)

    if is_empty(value):
        if not is_req:
            return True, None, None
        return False, f"Please provide {label}.", None

    value = str(value).strip()

    # Dropdown or other configured options
    options = valid_options or field_def.get("options")
    if options:
        norm_val = normalize(value)

        def _opt_str(opt):
            if isinstance(opt, dict):
                return str(opt.get("name") or opt.get("label") or opt.get("code") or "")
            return str(opt)

        # 1. Exact normalized match
        for option in options:
            if norm_val == normalize(_opt_str(option)):
                return True, None, option

        # 2. Tolerant contains / substring match (e.g. 'link colony' for 'Link Colony Phase 1', 'jor' for 'Jor Bagh')
        for option in options:
            norm_opt = normalize(_opt_str(option))
            if len(norm_val) >= 3 and norm_val in norm_opt:
                return True, None, option

        # 3. High-confidence word typo match (>= 85% similarity, e.g. 'horading' -> 'Hoarding Board')
        for option in options:
            opt_str = _opt_str(option)
            words = re.findall(r"[a-zA-Z0-9]+", opt_str)
            for w in words:
                if len(w) >= 4 and difflib.SequenceMatcher(None, norm_val, w.lower()).ratio() >= 0.85:
                    return True, None, option
            if difflib.SequenceMatcher(None, norm_val, normalize(opt_str)).ratio() >= 0.85:
                return True, None, option

        return False, f"Choose a valid option for {label}.", None

    # Dates
    if field_type == "date":
        return validate_date(field_def, value, label=label, draft=draft)

    # Mobile number (reads pattern from config.yml)
    if field_type in ("mobile", "phone") or field_id in ("mobileNumber", "phone_number"):
        mobile = re.sub(r"[\s\-+]", "", value)
        if mobile.startswith("91") and len(mobile) == 12:
            mobile = mobile[2:]
        elif mobile.startswith("0") and len(mobile) == 11:
            mobile = mobile[1:]

        pattern = validation_cfg.get("pattern")
        if pattern and not re.fullmatch(pattern, mobile):
            return False, "Enter a valid 10-digit Indian mobile number.", None

        return True, None, mobile

    # Email (reads pattern from config.yml, domain case-normalized)
    if field_type == "email" or field_id in ("emailId", "email"):
        pattern = validation_cfg.get("pattern")
        if pattern and not re.fullmatch(pattern, value):
            return False, "Enter a valid email address.", None

        if "@" in value:
            user_part, domain_part = value.rsplit("@", 1)
            cleaned_email = f"{user_part}@{domain_part.lower()}"
        else:
            cleaned_email = value
        return True, None, cleaned_email

    # Pattern check from config.yml
    pattern = validation_cfg.get("pattern")
    if pattern and not re.fullmatch(pattern, value):
        return False, f"Invalid format for {label}.", None

    # Length checks from config.yml
    min_len = validation_cfg.get("min_length")
    if min_len and len(value) < min_len:
        return False, f"{label} must be at least {min_len} characters.", None

    max_len = validation_cfg.get("max_length")
    if max_len and len(value) > max_len:
        return False, f"{label} must be at most {max_len} characters.", None

    return True, None, value
