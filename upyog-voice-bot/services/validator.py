
import re
from datetime import date, datetime


def is_empty(value):
    """Check whether a value is empty."""
    return value is None or str(value).strip().lower() in ("", "null", "none")


def normalize(value):
    """Ignore spaces and underscores when matching options."""
    return re.sub(r"[\s_]+", "", str(value).lower())


def validate_date(field_id, value, label, draft):
    """Validate a date and its relationship to other dates."""
    try:
        parsed = datetime.strptime(value, "%Y-%m-%d").date()
    except ValueError:
        return False, f"Enter {label} in YYYY-MM-DD format.", None

    if parsed <= date.today():
        return False, f"{label} must be tomorrow or later.", None

    if field_id == "end_date" and draft and draft.get("start_date"):
        try:
            start = datetime.strptime(
                str(draft["start_date"]).strip(), "%Y-%m-%d"
            ).date()
            if parsed < start:
                return False, "To Date cannot be earlier than From Date.", None
        except ValueError:
            return False, "Please correct the From Date first.", None

    return True, None, value


def validate_field(field_def, value, draft=None, valid_options=None):
    """Validate one field and return (valid, error, cleaned_value)."""
    field_id = field_def.get("id", "")
    label = field_def.get("label", field_id)
    field_type = field_def.get("type", "text")

    if is_empty(value):
        return False, f"Please provide {label}.", None

    value = str(value).strip()

    # Dropdown or other configured options
    options = valid_options or field_def.get("options")
    if options:
        for option in options:
            if normalize(value) == normalize(option):
                return True, None, option

        return False, f"Choose a valid option for {label}.", None

    # Dates
    if field_type == "date":
        return validate_date(field_id, value, label, draft)

      # Mobile number (reads pattern from config.yml)
    if field_id in ("mobileNumber", "phone_number"):
        mobile = re.sub(r"[\s\-+]", "", value)
        if mobile.startswith("91") and len(mobile) == 12:
            mobile = mobile[2:]
        elif mobile.startswith("0") and len(mobile) == 11:
            mobile = mobile[1:]

        pattern = field_def.get("validation", {}).get("pattern")
        if pattern and not re.fullmatch(pattern, mobile):
            return False, "Enter a valid 10-digit Indian mobile number.", None

        return True, None, mobile

    # Email (reads pattern from config.yml)
    if field_id in ("emailId", "email"):
        pattern = field_def.get("validation", {}).get("pattern")
        if pattern and not re.fullmatch(pattern, value):
            return False, "Enter a valid email address.", None

        return True, None, value.lower()

    # Pattern check from config.yml ( applicantName pattern: "^[a-zA-Z ]+$")
    pattern = field_def.get("validation", {}).get("pattern")
    if pattern and not re.fullmatch(pattern, value):
        return False, f"Invalid format for {label}.", None

    # Length checks from config.yml ( min_length: 5 for address)
    min_len = field_def.get("validation", {}).get("min_length")
    if min_len and len(value) < min_len:
        return False, f"{label} must be at least {min_len} characters.", None

    max_len = field_def.get("validation", {}).get("max_length")
    if max_len and len(value) > max_len:
        return False, f"{label} must be at most {max_len} characters.", None

    return True, None, value
