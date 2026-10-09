# ============== WORKFLOW & FORM EXTRACTION PROMPTS ==============
import json
from typing import List, Dict, Any, Optional


# ─── Advertisement Booking Prompts ───────────────────────────────────

def build_adv_extract_prompt(
    llm_extractable_fields: List[str],
    schema_hints: Dict[str, str],
    draft_booking: Dict[str, Any],
    last_assistant_msg: str,
    user_msg: str
) -> str:
    """Builds prompt for extracting advertisement booking form fields from user messages."""
    return f"""You are a strict data extraction AI for UPYOG Advertisement bookings.
Your ONLY job is to extract ANY updated or newly specified fields from the user's message.
Target fields to extract: {json.dumps(llm_extractable_fields)}
Hints: {json.dumps(schema_hints)}

Already collected fields: {json.dumps(draft_booking)}
Question the user is answering: "{last_assistant_msg}"
User message: "{user_msg}"

CRITICAL RULES:
- If the user's message clearly answers or updates one or more target fields, extract them.
- If a field is not mentioned or changed, return null for that field.
- If the user just says "Yes" or "No", use the "Question the user is answering" to figure out which field they are answering (e.g., nightLight).
- STRICT RULE FOR 'addType': Only extract if they mention a specific type (e.g. Hoarding, Unipole, Kiosk, Banner). Do NOT extract generic words like 'adv' or 'advertisement' as addType!

Reply ONLY with valid JSON containing the extracted fields. No explanations.
Example:
{{
  "location": "extracted value",
  "address": null
}}"""


def build_adv_question_prompt(
    draft_booking: Dict[str, Any],
    field_label: str,
    options: List[str],
    user_msg: str,
    lang_rule: str
) -> str:
    """Builds prompt for generating natural, conversational questions for the next missing ad booking field."""
    return f"""You are a conversational UPYOG advertisement booking concierge.
Collected so far: {json.dumps({k: v for k, v in draft_booking.items() if v})}
Your task: Ask the user to provide the next missing field: "{field_label}".
Available options for this field: {json.dumps(options) if options else "None (free text/date/upload)"}

User just said: "{user_msg}"

Instructions:
1. Language: {lang_rule}
2. Generate a natural, polite, conversational question asking for the "{field_label}".
3. Note: "Advertisement Type" refers to outdoor municipal advertising structure types (such as Hoarding, Unipole, Kiosk, Billboard, Banner, Poster, Digital Screen). NEVER ask about or refer to media file formats like videos, images, or audio formats.
4. Do NOT say "Hello" unless the user explicitly greeted you.
5. NEVER repeat or confirm what the user just selected. 
6. DO NOT list the available options in the text (the UI will handle that).
7. Maintain a formal, polite, professional tone. Never use informal or familial terms of address.
8. Output ONLY the conversational question text."""


def build_adv_past_booking_filter_prompt(user_msg: str) -> str:
    """Builds prompt to extract date or ID filters when searching past advertisement bookings."""
    return f"""Extract any dates or booking IDs from this query to filter past bookings.
Query: '{user_msg}'
Return ONLY a JSON object (no other text) with:
- "date_str": A string representing the exact date in YYYY-MM-DD format if mentioned, else null.
- "booking_id": The exact ADV-... ID, or just the partial digits (like the last 4 numbers) if mentioned, else null."""


# ─── Grievance Filing Prompts ────────────────────────────────────────

def build_grievance_extract_prompt(
    remaining_missing: List[str],
    schema_hints: Dict[str, str],
    collected_str: str,
    last_ai: str,
    user_msg: str
) -> str:
    """Builds prompt for extracting grievance form fields (category, locality, description)."""
    return f"""You are a data extraction AI for UPYOG Grievance filing.
Extract missing form field values from the user's message.

Missing fields: {json.dumps(remaining_missing)}
Field hints & valid options: {json.dumps(schema_hints)}
Already collected fields: {collected_str}
Bot's last question: "{last_ai}"
User's message: "{user_msg}"

Rules:
1. If the user answered the missing field in their message, extract the exact value.
2. For 'category', 'sub_category', and 'locality', ONLY extract if it matches one of the valid options. DO NOT invent or accept arbitrary strings for locality.
3. For 'description', extract the user's description of their problem/request (e.g. 'i want a refund', 'water leakage', 'streetlight damaged').
4. Return null for any field not answered or not found in options.
5. NEVER extract meta or navigation words like 'continue', 'resume', 'draft', 'show drafts', 'my drafts' as any field value.

Reply ONLY with valid JSON:
{{{', '.join(f'"{f}": "extracted value or null"' for f in remaining_missing)}}}"""


def build_grievance_question_prompt(
    draft: Dict[str, Any],
    field_label: str,
    field_id: str,
    options: List[str],
    user_msg: str,
    lang_rule: str
) -> str:
    """Builds prompt for generating natural questions for the next missing grievance field."""
    return f"""You are a UPYOG grievance filing assistant collecting a specific form field.
Collected so far: {json.dumps({k: v for k, v in draft.items() if v and not k.startswith("_")})}
Next field to collect: "{field_label}" (field id: "{field_id}")
Available options: {json.dumps(options) if options else "Free text -- user can type anything"}
User's last message: "{user_msg}"

CRITICAL INSTRUCTIONS:
1. Language: {lang_rule}
2. Ask ONLY for the "{field_label}" field. Do NOT ask anything else.
3. Do NOT ask follow-up questions about duration, impact, or history.
4. Do NOT list the options in text (the UI dropdown will show them).
5. If field is 'description': ask the user to briefly describe the problem they are facing.
6. Keep it to 1-2 sentences. Output ONLY the question, nothing else.
7. Use **bold** for the field name when mentioning it.
8. Maintain a formal, polite, professional tone. Never use informal or familial terms of address."""


# ─── Address Parser Prompt ──────────────────────────────────────────

def build_address_parser_prompt(address_str: str) -> str:
    """Builds prompt for parsing full address strings into structured postal and locality attributes."""
    return f"""You are a strict data parser for UPYOG addresses.
Given this full address: "{address_str}"
Extract these fields as a JSON object:
- "pincode": 6-digit postal code (e.g. "110001", "180091")
- "city": city name
- "locality": locality name
- "streetName": street name
- "houseNo": house number or building number (e.g. "E-56", "23")
- "landmark": landmark if present, else null

Reply with ONLY the valid JSON object (no markdown, no other text)."""
