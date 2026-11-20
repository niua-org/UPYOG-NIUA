"""
Central NLU Lexicon for UPYOG Voice Bot.

Provides standardized, high-performance vocabulary sets and intent classifiers
for user negations, cancellations, confirmations, and workflow restart triggers.
Used across chat handlers, grievance, and advertisement booking workflows.
"""

import re
from typing import Set

# ====================================================================
# 1. PURE REFUSAL & BARE NEGATIONS
# ====================================================================
# Matches single-word or short refusal answers (e.g., when asked for an optional/mandatory field)
BARE_NEGATIONS: Set[str] = {
    "no", "nope", "nah", "naw", "na", "nahi", "nahin", "na ji", "never",
    "none", "not now", "not really", "nothing", "nopes", "mat karo",
    "dont", "don't", "no thanks", "no please"
}

# ====================================================================
# 2. START NEW WORKFLOW / RESTART TRIGGERS
# ====================================================================
# Matches explicit requests to start fresh, discard an unconfirmed draft, or file a new application
START_NEW_WORKFLOW_REGEX = re.compile(
    r'\b(?:new|another|fresh|start\s+over|restart|start\s+new|re-?file)\s+(?:complaint|grievance|application|booking|ad)\b|'
    r'\b(?:file|register|lodge|start|create|make)\s+(?:another|a\s+new|new|fresh)\s+(?:complaint|grievance|application|booking|ad)\b|'
    r'\b(?:start\s+over|restart|start\s+new|start\s+fresh|naya\s+complaint|nayi\s+shikayat|shuruaat\s+karo|shuru\s+se)\b',
    re.IGNORECASE
)

# ====================================================================
# 3. EXPLICIT WORKFLOW CANCELLATION & WITHDRAWAL
# ====================================================================
# Matches explicit commands to cancel, abort, or withdraw the current application
CANCEL_COMMAND_REGEX = re.compile(
    r'\b(?:cancel|withdraw|abort|leave\s+it|rehne\s+do|chhod\s+do|chodo|quit|exit|stop|discard)\b|'
    r'\b(?:clear\s+draft|delete\s+draft|cancel\s+draft|cancel\s+booking|cancel\s+complaint)\b|'
    r'\b(?:don\'t|dont|do\s+not|nahi)\s+(?:want\s+to\s+)?(?:file|submit|book|continue|proceed|karna|chahiye)\b',
    re.IGNORECASE
)

# ====================================================================
# 4. AFFIRMATIVE & CONFIRMATION
# ====================================================================
AFFIRMATIVE_WORDS: Set[str] = {
    "yes", "yeah", "yup", "yep", "sure", "ok", "okay", "fine", "proceed",
    "confirm", "submit", "haan", "ha", "sahi", "theek", "bilkul", "zaroor",
    "1", "option 1"
}

CONFIRM_ACTION_REGEX = re.compile(
    r'\b(?:confirm\s*&\s*submit|submit\s+complaint|confirm\s+complaint|submit\s+booking|'
    r'confirm\s+booking|submit\s+it|file\s+it|book\s+it|please\s+submit|please\s+confirm)\b',
    re.IGNORECASE
)

# ====================================================================
# 5. CONVENIENCE MATCHER FUNCTIONS
# ====================================================================

def is_bare_negation(text: str) -> bool:
    """Returns True if the cleaned input is a pure refusal/negation like 'no', 'nahi'."""
    clean = re.sub(r'[\s_]+', ' ', (text or "").strip().lower())
    norm = re.sub(r'[\s_]+', '', clean)
    return clean in BARE_NEGATIONS or norm in BARE_NEGATIONS

def is_start_new_workflow(text: str) -> bool:
    """Returns True if user asks to start another complaint, new booking, or start over."""
    clean = (text or "").strip().lower()
    return bool(START_NEW_WORKFLOW_REGEX.search(clean))

def is_explicit_cancel(text: str) -> bool:
    """Returns True if user explicitly requests to cancel/withdraw/abandon the workflow."""
    clean = (text or "").strip().lower()
    return bool(CANCEL_COMMAND_REGEX.search(clean))

def is_affirmative(text: str) -> bool:
    """Returns True if user confirms/submits."""
    clean = re.sub(r'[\s_]+', ' ', (text or "").strip().lower())
    return clean in AFFIRMATIVE_WORDS or bool(CONFIRM_ACTION_REGEX.search(clean))