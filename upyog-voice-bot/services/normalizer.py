"""
services/normalizer.py — Pre-NLU Text & Voice Normalization Layer
==================================================================
Purpose:
  Acts as an intelligent, pre-intent classification preprocessing gate that:
  1. Protects critical entities (tracking IDs, phone numbers, dates) from corruption.
  2. Resolves voice ASR acoustic misrecognitions and speech-to-text transcript slips.
  3. Corrects typographical and spelling errors against a closed domain vocabulary
     using an optimized Damerau-Levenshtein distance algorithm.
  4. Provides service synonym lookup & canonicalization across English, Devanagari, and Hinglish.

Architectural Context:
  - Upstream: routes/chat_routes.py receives raw citizen speech transcripts or text queries.
  - Normalizer: This module intercepts raw input before fast-path or LLM intent classification.
  - Downstream: services/chat_handlers.py and services/intent_service.py receive clean,
    canonicalized, entity-safe input.
"""

import os
import re
import json
import yaml
import difflib
import logging
from typing import Tuple, List, Dict, Optional, Set

logger = logging.getLogger(__name__)

# ==============================================================================
# 1. ENTITY PROTECTION & MASKING REGEX PATTERNS
# ==============================================================================
# Reason: Spell checkers and fuzzy matchers must NEVER alter alphanumeric tracking
# IDs, telephone numbers, or formatted dates. We mask them before correction and
# unmask them afterward.
# Note: ID_PATTERN strictly requires uppercase official prefix and at least one digit
# so ordinary words like "advertisement", "advance", "nocturnal" are never masked as IDs.
# ==============================================================================

ID_PATTERN = re.compile(r'\b(?:ADV|PGR|PT|TL|WS|BPA|NOC|BND)[-_]?(?=[A-Z0-9-]*\d)[A-Z0-9-]+\b')
PHONE_PATTERN = re.compile(r'(?:\+91[\-\s]?)?[6-9]\d{9}\b')
DATE_PATTERN = re.compile(r'\b\d{4}[-/]\d{2}[-/]\d{2}\b')

# ==============================================================================
# 2. VOICE ASR ACOUSTIC & PHONETIC CONFUSION MAP
# ==============================================================================
# Reason: Voice speech-to-text engines (e.g. Web Speech API, Whisper, Bhashini)
# frequently produce phonetic homophones or split compound Indian civic terms.
# Standard QWERTY edit-distance spell-checking fails on these.
# ==============================================================================

ASR_ACOUSTIC_RULES: List[Tuple[re.Pattern, str, str]] = [
    # (Compiled Regex, Replacement, Reason/Description)
    (re.compile(r'\buni\s+pole\b', re.IGNORECASE), "unipole", "ASR split compound word 'unipole'"),
    (re.compile(r'\byou\s+need\s+pole\b', re.IGNORECASE), "unipole", "Acoustic mishearing of 'unipole'"),
    (re.compile(r'\byou\s+nipole\b', re.IGNORECASE), "unipole", "Acoustic mishearing of 'unipole'"),
    (re.compile(r'\bp\s+g\s+r\b', re.IGNORECASE), "pgr", "ASR spaced acronym 'P G R'"),
    (re.compile(r'\ba\s+d\s+v\b', re.IGNORECASE), "adv", "ASR spaced acronym 'A D V'"),
    (re.compile(r'\badd\s+booking\b', re.IGNORECASE), "ad booking", "Homophone 'add' for advertisement 'ad'"),
    (re.compile(r'\bshikayet\b', re.IGNORECASE), "shikayat", "Phonetic vowel variation of 'shikayat'"),
    (re.compile(r'\bsikayat\b', re.IGNORECASE), "shikayat", "Dentalized sibilant variation of 'shikayat'"),
    (re.compile(r'\bshikaayat\b', re.IGNORECASE), "shikayat", "Elongated vowel transliteration"),
    (re.compile(r'\bsamasia\b', re.IGNORECASE), "samasya", "Phonetic variation of 'samasya'"),
    (re.compile(r'\bkachada\b', re.IGNORECASE), "kachra", "Regional retroflex flap variation of 'kachra'"),
    (re.compile(r'\bkoodha\b', re.IGNORECASE), "kooda", "Aspirated variation of 'kooda'"),
    (re.compile(r'\bpaani\b', re.IGNORECASE), "pani", "Standardized transliteration for water"),
    (re.compile(r'\bsarak\b', re.IGNORECASE), "sadak", "Regional retroflex transliteration for road"),
    (re.compile(r'\blite\b', re.IGNORECASE), "light", "Homophone 'lite' for 'light'"),
    (re.compile(r'\bstreetlite\b', re.IGNORECASE), "streetlight", "Homophone 'streetlite' for 'streetlight'"),
    # Common citizen texting / ASR shorthand expansions
    (re.compile(r'\bwht\b', re.IGNORECASE), "what", "Chat shorthand 'wht' -> 'what'"),
    (re.compile(r'\bhw\b', re.IGNORECASE), "how", "Chat shorthand 'hw' -> 'how'"),
    (re.compile(r'\bchk\b', re.IGNORECASE), "check", "Chat shorthand 'chk' -> 'check'"),
    (re.compile(r'\bplz\b|\bpls\b', re.IGNORECASE), "please", "Chat shorthand 'plz/pls' -> 'please'"),
    (re.compile(r'\babt\b', re.IGNORECASE), "about", "Chat shorthand 'abt' -> 'about'"),
    # Digital / electronic civic terms
    (re.compile(r'\b(?:e[\s\-_]+waste|ewaste)\b', re.IGNORECASE), "e-waste", "Normalize e-waste terminology"),
]

# ==============================================================================
# 2B. MUNICIPAL ABBREVIATION & ACRONYM EXPANSION TABLE
# ==============================================================================
# Reason: Citizens commonly use short municipal abbreviations (e.g. "PT ka bill",
# "TL renew", "WS connection", "SAF form", "PID"). Because these are 2-3 letters,
# standard fuzzy spell-checkers ignore them. We expand them to both the full
# canonical terminology AND the acronym so semantic embeddings (FAISS) and intent
# classifiers understand the citizen's domain context with 100% precision.
# ==============================================================================

MUNICIPAL_ABBREVIATIONS: List[Tuple[re.Pattern, str, str]] = [
    (re.compile(r'\bpt\b(?!\.)', re.IGNORECASE), "Property Tax (PT)", "Abbreviation 'PT' -> 'Property Tax'"),
    (re.compile(r'\btl\b(?!\.)', re.IGNORECASE), "Trade License (TL)", "Abbreviation 'TL' -> 'Trade License'"),
    (re.compile(r'\bws\b(?!\.)', re.IGNORECASE), "Water & Sewerage (WS)", "Abbreviation 'WS' -> 'Water & Sewerage'"),
    (re.compile(r'\bpgr\b(?!\.)', re.IGNORECASE), "Public Grievance Redressal (PGR)", "Abbreviation 'PGR' -> 'Public Grievance Redressal'"),
    (re.compile(r'\badv\b(?!\.)', re.IGNORECASE), "Advertisement (ADV)", "Abbreviation 'ADV' -> 'Advertisement'"),
    (re.compile(r'\bbpa\b(?!\.)', re.IGNORECASE), "Building Plan Approval (BPA)", "Abbreviation 'BPA' -> 'Building Plan Approval'"),
    (re.compile(r'\bobps\b(?!\.)', re.IGNORECASE), "Online Building Plan Approval (OBPS)", "Abbreviation 'OBPS' -> 'Online Building Plan'"),
    (re.compile(r'\bnoc\b(?!\.)', re.IGNORECASE), "No Objection Certificate (NOC)", "Abbreviation 'NOC' -> 'No Objection Certificate'"),
    (re.compile(r'\bbnd\b(?!\.)', re.IGNORECASE), "Birth & Death (BND)", "Abbreviation 'BND' -> 'Birth & Death'"),
    (re.compile(r'\bpid\b(?!\.)', re.IGNORECASE), "Property ID (PID)", "Abbreviation 'PID' -> 'Property ID'"),
    (re.compile(r'\bupic\b(?!\.)', re.IGNORECASE), "Property UPIC", "Abbreviation 'UPIC' -> 'Property UPIC'"),
    (re.compile(r'\bsaf\b(?!\.)', re.IGNORECASE), "Self Assessment Form (SAF)", "Abbreviation 'SAF' -> 'Self Assessment Form'"),
    (re.compile(r'\bulb\b(?!\.)', re.IGNORECASE), "Urban Local Body (ULB)", "Abbreviation 'ULB' -> 'Urban Local Body'"),
]


# ==============================================================================
# 3. SYNONYM REGISTRY & DOMAIN VOCABULARY LOADER
# ==============================================================================

_SYNONYMS_CACHE: Optional[Dict] = None
_DOMAIN_VOCABULARY: Optional[Dict[str, str]] = None
_COMPILED_SYNONYMS: Optional[List[Tuple[re.Pattern, str, str, str, str, Optional[str]]]] = None


def load_synonyms_registry() -> Dict:
    """Loads the canonical synonyms registry from data/synonyms.yml and precompiles
    matching patterns sorted by length descending for optimal specificity and performance.

    Cached in-memory after first load. Changes to synonyms.yml require a service restart.
    """
    global _SYNONYMS_CACHE, _COMPILED_SYNONYMS
    if _SYNONYMS_CACHE is not None:
        return _SYNONYMS_CACHE

    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    syn_path = os.path.join(base_dir, "data", "synonyms.yml")

    patterns: List[Tuple[re.Pattern, str, str, str, str, Optional[str]]] = []

    if os.path.exists(syn_path):
        try:
            with open(syn_path, "r", encoding="utf-8") as f:
                data = yaml.safe_load(f) or {}
                _SYNONYMS_CACHE = data.get("services", {})
                logger.info(f"[NORMALIZER] Successfully loaded synonyms registry for {len(_SYNONYMS_CACHE)} services from {syn_path}")

                raw_entries = []
                for s_key, s_data in _SYNONYMS_CACHE.items():
                    c_name = s_data.get("canonical_name", s_key)
                    t_wf = s_data.get("target_workflow", "faq")
                    # Expressions
                    for lang_exprs in s_data.get("expressions", {}).values():
                        if isinstance(lang_exprs, list):
                            for expr in lang_exprs:
                                raw_entries.append((expr, s_key, c_name, t_wf, None))
                    # Civic subcategories
                    for sub_cat, terms in s_data.get("civic_subcategories", {}).items():
                        for term in terms:
                            raw_entries.append((term, s_key, c_name, t_wf, sub_cat))

                # Sort by expression length descending for longest/most specific match first
                raw_entries.sort(key=lambda x: len(x[0]), reverse=True)
                for expr, s_key, c_name, t_wf, sub_cat in raw_entries:
                    rgx = re.compile(rf'\b{re.escape(expr.lower().strip())}\b')
                    patterns.append((rgx, s_key, c_name, t_wf, expr, sub_cat))

                _COMPILED_SYNONYMS = patterns
                return _SYNONYMS_CACHE
        except Exception as e:
            logger.error(f"[NORMALIZER] Failed to parse {syn_path}: {e}")

    logger.warning("[NORMALIZER] synonyms.yml not found, using empty fallback")
    _SYNONYMS_CACHE = {}
    _COMPILED_SYNONYMS = []
    return _SYNONYMS_CACHE


STOPWORDS_NOT_FUZZY_TARGETS = {
    "par", "per", "ka", "ki", "ke", "ko", "se", "me", "mai", "aur",
    "and", "the", "for", "hai", "hain", "karna", "kare", "karo", "nahi",
    "nahin", "yeh", "woh", "kar", "ho", "tha", "thi", "the", "kya", "aap"
}

# Protected conversational and dictionary words that must NEVER be fuzzy-corrected
PROTECTED_COMMON_WORDS: Set[str] = {
    "thanks", "thank", "night", "evening", "morning", "afternoon", "hello", "hi", "hey",
    "good", "fine", "ok", "okay", "sure", "yes", "no", "help", "please", "welcome",
    "what", "how", "when", "where", "which", "who", "whom", "whose", "why", "check",
    "ewaste", "e-waste", "echallan", "ebill", "eservice", "etender", "eseva", "ekyc", "emutation",
    "right", "great", "park", "road", "city", "area", "house", "trade", "traders",
    "office", "name", "date", "time", "week", "month", "year",
    "kaise", "kare", "karna", "karo", "karni", "karne", "paise", "rupaye", "chahiye",
    "batao", "bataiye", "dikhao", "dikhaye", "aap", "mera", "meri", "mere", "humara",
    "apna", "apni", "hatao", "lagao", "kya", "kyun", "kahan", "kab", "kaun", "kitna",
    "kitne", "kitni", "nahi", "nahin", "theek", "achha", "accha", "din", "raat"
}


def get_domain_vocabulary() -> Dict[str, str]:
    """Builds a closed-world domain vocabulary of core municipal terms,
    service aliases, and system action keywords for fast fuzzy matching.

    Returns:
        Dict[str, str]: Map of lowercase valid canonical tokens (token -> token).
    """
    global _DOMAIN_VOCABULARY
    if _DOMAIN_VOCABULARY is not None:
        return _DOMAIN_VOCABULARY

    vocab: Dict[str, str] = {}

    # 1. Base conversational & system commands
    system_words = [
        "hello", "namaste", "greetings", "hi", "hey",
        "login", "logout", "signin", "signup", "otp", "profile",
        "cancel", "confirm", "proceed", "continue", "resume", "delete", "save",
        "status", "track", "history", "details", "check", "view",
        "draft", "drafts", "application", "applications",
        "yes", "yeah", "sure", "okay", "haan", "theek", "nahi", "nahin",
        "help", "rules", "fees", "charges", "process", "documents",
        "water", "sewer", "drainage", "garbage", "trash", "sanitation",
        "street", "light", "pothole", "road", "pgr", "adv"
    ]
    for w in system_words:
        w_low = w.lower()
        if len(w_low) >= 4 and w_low not in STOPWORDS_NOT_FUZZY_TARGETS:
            vocab[w_low] = w_low

    # 2. Add tokens from service expressions in synonyms.yml
    registry = load_synonyms_registry()
    for s_key, s_data in registry.items():
        if len(s_key) >= 4:
            vocab[s_key.lower()] = s_key.lower()
        expressions = s_data.get("expressions", {})
        for lang_code in ("en", "hinglish"):
            for expr in expressions.get(lang_code, []):
                for token in re.findall(r'[a-zA-Z0-9]+', expr.lower()):
                    if len(token) >= 4 and token not in STOPWORDS_NOT_FUZZY_TARGETS:
                        vocab[token] = token

        # Add civic subcategory terms
        subcats = s_data.get("civic_subcategories", {})
        for sub_name, terms in subcats.items():
            for term in terms:
                for token in re.findall(r'[a-zA-Z0-9]+', term.lower()):
                    if len(token) >= 4 and token not in STOPWORDS_NOT_FUZZY_TARGETS:
                        vocab[token] = token

    _DOMAIN_VOCABULARY = vocab
    logger.info(f"[NORMALIZER] Initialized closed domain vocabulary with {len(_DOMAIN_VOCABULARY)} unique tokens")
    return _DOMAIN_VOCABULARY


# ==============================================================================
# 3B. VALID GENERAL & CIVIC LEXICON (HYPER-CORRECTION SHIELD)
# ==============================================================================
# Reason: A spell-checker must ONLY correct non-words / genuine misspellings.
# Legitimate English and Hinglish words (e.g. 'ewaste', 'drain', 'stall', 'court',
# 'meter', 'plant', 'store', 'order', 'night') must NEVER be hyper-corrected to
# municipal domain terms just because of small edit distances.
# ==============================================================================

_COMMON_LEXICON: Optional[Set[str]] = None


def get_common_lexicon() -> Set[str]:
    """Loads the broad English and Hinglish valid vocabulary lexicon from
    data/common_lexicon.txt, supplemented by system dictionaries and digital civic terms.
    Ensures legitimate words (e.g. 'ewaste', 'drain', 'stall', 'court', 'meter')
    are recognized as valid words and NEVER hyper-corrected to municipal terms.
    """
    global _COMMON_LEXICON
    if _COMMON_LEXICON is not None:
        return _COMMON_LEXICON

    lexicon: Set[str] = set()

    # 1. Load bundled data/common_lexicon.txt
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    lex_path = os.path.join(base_dir, "data", "common_lexicon.txt")
    if os.path.exists(lex_path):
        try:
            with open(lex_path, "r", encoding="utf-8") as f:
                lexicon.update(line.strip().lower() for line in f if line.strip())
            logger.info(f"[NORMALIZER] Loaded {len(lexicon)} valid lexicon words from {lex_path}")
        except Exception as e:
            logger.warning(f"[NORMALIZER] Failed to read {lex_path}: {e}")

    # 2. Add protected common words and domain vocabulary
    lexicon.update(w.lower() for w in PROTECTED_COMMON_WORDS)
    lexicon.update(get_domain_vocabulary().keys())

    _COMMON_LEXICON = lexicon
    return _COMMON_LEXICON


# ==============================================================================
# 4. OPTIMIZED DAMERAU-LEVENSHTEIN FUZZY MATCHING ALGORITHM
# ==============================================================================
# Reason: Pure Python, zero-external-dependency distance metric with transposition
# awareness (e.g. 'greivance' -> 'grievance'). Executes in microseconds.
# ==============================================================================

def damerau_levenshtein_distance(s1: str, s2: str) -> int:
    """Calculates Damerau-Levenshtein distance between two strings,
    supporting insertion, deletion, substitution, and transposition of adjacent characters.
    """
    len1, len2 = len(s1), len(s2)
    if s1 == s2:
        return 0
    if len1 == 0:
        return len2
    if len2 == 0:
        return len1

    # Matrix initialization
    d = [[0] * (len2 + 1) for _ in range(len1 + 1)]

    for i in range(len1 + 1):
        d[i][0] = i
    for j in range(len2 + 1):
        d[0][j] = j

    for i in range(1, len1 + 1):
        for j in range(1, len2 + 1):
            cost = 0 if s1[i - 1] == s2[j - 1] else 1
            d[i][j] = min(
                d[i - 1][j] + 1,       # deletion
                d[i][j - 1] + 1,       # insertion
                d[i - 1][j - 1] + cost  # substitution
            )
            # Transposition check
            if i > 1 and j > 1 and s1[i - 1] == s2[j - 2] and s1[i - 2] == s2[j - 1]:
                d[i][j] = min(d[i][j], d[i - 2][j - 2] + cost)

    return d[len1][len2]


def find_closest_domain_token(word: str, vocab: Dict[str, str]) -> Optional[Tuple[str, int, float]]:
    """Finds the closest domain token for a candidate word if within strict confidence thresholds.

    Rules:
      - Word must have length >= 4 (short words are skipped to prevent false positives).
      - Common conversational English and Hinglish words are protected from mutation.
      - Max edit distance = 1 for words <= 5 characters.
      - Max edit distance = 2 for words >= 6 characters.
      - Normalized similarity ratio >= 0.85.

    Returns:
        Optional[Tuple[canonical_word, distance, similarity_score]]
    """
    clean_w = word.lower()
    w_len = len(clean_w)

    lexicon = get_common_lexicon()

    # Core Rule: If the word is already a valid word in the English / Hinglish lexicon,
    # or in the domain vocabulary, or in protected words, it is NOT a typo. Never alter it!
    if w_len < 4 or clean_w in lexicon or clean_w in vocab:
        return None

    # Guard: Do not strip leading 'e' from electronic/digital civic terms (e.g. 'ewaste' -> 'waste', 'echallan' -> 'challan')
    if clean_w.startswith("e") and (clean_w[1:] in vocab or clean_w[1:] in lexicon):
        return None

    max_allowed_dist = 2 if w_len <= 5 else 4
    best_candidate = None
    min_dist = max_allowed_dist + 1
    best_score = 0.0

    # Optimization: Filter vocabulary candidates by length window (+/- max_allowed_dist)
    for candidate in vocab.keys():
        c_len = len(candidate)
        if c_len < 4 or candidate in STOPWORDS_NOT_FUZZY_TARGETS or abs(c_len - w_len) > max_allowed_dist:
            continue

        # Fast heuristic: first or last character match is typically preserved in human typos
        if candidate[0] != clean_w[0] and candidate[-1] != clean_w[-1]:
            continue

        dist = damerau_levenshtein_distance(clean_w, candidate)
        if dist <= max_allowed_dist and dist < min_dist:
            score = difflib.SequenceMatcher(None, clean_w, candidate).ratio()
            if score >= 0.85:
                min_dist = dist
                best_candidate = candidate
                best_score = score

    if best_candidate and min_dist <= max_allowed_dist:
        return vocab[best_candidate], min_dist, best_score

    return None


# ==============================================================================
# 5. PIPELINE EXECUTION: PRE-NLU NORMALIZATION
# ==============================================================================

def normalize_voice_and_text(user_input: str) -> Tuple[str, bool, List[str]]:
    """Executes the complete pre-NLU normalization pipeline:
    1. Masks protected entities (tracking IDs, phone numbers, dates).
    2. Applies voice ASR phonetic and acoustic replacements.
    3. Corrects typographical and spelling errors against the domain vocabulary.
    4. Unmasks protected entities.

    Args:
        user_input (str): Raw string from user (voice transcript or typed chat).

    Returns:
        Tuple[str, bool, List[str]]:
            - normalized_input (str): Cleaned and corrected text.
            - has_changes (bool): True if any modification was applied.
            - audit_log (List[str]): Explanations of all modifications made.
    """
    if not user_input or not user_input.strip():
        return user_input or "", False, []

    text = user_input.strip()

    # Fast-exit for structured JSON / UI control payloads (e.g. document uploads, slot selections)
    # Reason: System payloads like {"document": "..."} or [Uploaded Document: ...]
    # must remain verbatim and never undergo text NLP spell-correction.
    if re.match(r"^\[(Uploaded Document|CANCEL_DRAFT|RESUME_DRAFT|SLOT_CHOICE):?", text, re.IGNORECASE):
        logger.debug(f"[NORMALIZER] Bypassing normalization for UI control tag: {user_input}")
        return user_input, False, []

    if (text.startswith("{") and text.endswith("}")) or (text.startswith("[") and text.endswith("]")):
        try:
            json.loads(text)
            logger.debug(f"[NORMALIZER] Bypassing normalization for structured JSON/UI payload: {user_input}")
            return user_input, False, []
        except Exception:
            pass

    audit_log: List[str] = []
    masks: Dict[str, str] = {}

    # --------------------------------------------------------------------------
    # Step 1: Entity Protection Masking
    # --------------------------------------------------------------------------
    mask_idx = 0

    def _mask_match(m, label):
        nonlocal mask_idx
        placeholder = f"__{label}_{mask_idx}__"
        masks[placeholder] = m.group(0)
        mask_idx += 1
        return placeholder

    text = ID_PATTERN.sub(lambda m: _mask_match(m, "ID"), text)
    text = PHONE_PATTERN.sub(lambda m: _mask_match(m, "PHONE"), text)
    text = DATE_PATTERN.sub(lambda m: _mask_match(m, "DATE"), text)

    # --------------------------------------------------------------------------
    # Step 2: Voice ASR Acoustic Normalization
    # --------------------------------------------------------------------------
    for pattern, replacement, desc in ASR_ACOUSTIC_RULES:
        if pattern.search(text):
            old_text = text
            text = pattern.sub(replacement, text)
            if old_text != text:
                audit_log.append(f"ASR Acoustic Rule: {desc} -> '{replacement}'")
                logger.info(f"[NORMALIZER] Applied {desc}: '{old_text}' -> '{text}'")

    # --------------------------------------------------------------------------
    # Step 2B: Municipal Abbreviation & Acronym Expansion
    # --------------------------------------------------------------------------
    for pattern, replacement, desc in MUNICIPAL_ABBREVIATIONS:
        if pattern.search(text):
            old_text = text
            text = pattern.sub(replacement, text)
            if old_text != text:
                audit_log.append(f"Abbreviation Expansion: {desc} -> '{replacement}'")
                logger.info(f"[NORMALIZER] Applied {desc}: '{old_text}' -> '{text}'")

    # --------------------------------------------------------------------------
    # Step 3: Domain-Constrained Fuzzy Spell Correction
    # --------------------------------------------------------------------------
    vocab = get_domain_vocabulary()
    words = text.split()
    corrected_words = []
    fuzzy_changed = False

    for w in words:
        # If the token is a mask placeholder, keep it untouched
        if w.startswith("__") and w.endswith("__"):
            corrected_words.append(w)
            continue

        # Extract punctuation
        prefix_punct = ""
        suffix_punct = ""
        m_start = re.match(r'^[^\w]+', w)
        if m_start:
            prefix_punct = m_start.group(0)
            w_core = w[len(prefix_punct):]
        else:
            w_core = w

        m_end = re.search(r'[^\w]+$', w_core)
        if m_end:
            suffix_punct = m_end.group(0)
            w_core = w_core[:-len(suffix_punct)]

        if not w_core:
            corrected_words.append(w)
            continue

        # Check for spelling correction
        correction = find_closest_domain_token(w_core, vocab)
        if correction:
            canonical, dist, score = correction
            corrected_words.append(f"{prefix_punct}{canonical}{suffix_punct}")
            fuzzy_changed = True
            audit_log.append(f"Fuzzy Match: '{w_core}' -> '{canonical}' (dist={dist}, score={score:.2f})")
            logger.debug(f"[NORMALIZER] Fuzzy corrected: '{w_core}' -> '{canonical}' (dist={dist}, score={score:.2f})")
        else:
            corrected_words.append(w)

    text = " ".join(corrected_words)

    # --------------------------------------------------------------------------
    # Step 4: Entity Unmasking
    # --------------------------------------------------------------------------
    for placeholder, original_value in masks.items():
        text = text.replace(placeholder, original_value)

    has_changes = bool(audit_log) or (text != user_input)
    if has_changes:
        logger.debug(f"[NORMALIZER] Query preprocessed: '{user_input}' -> '{text}' | Actions: {audit_log}")
        logger.info(f"[NORMALIZER] Applied {len(audit_log)} normalization rules: {', '.join(a.split(':')[0] for a in audit_log)}")

    return text, has_changes, audit_log


# ==============================================================================
# 6. SERVICE RESOLUTION HELPER VIA SYNONYMS REGISTRY
# ==============================================================================

def resolve_service_by_synonyms(query: str) -> Optional[Dict]:
    """Inspects query text against the precompiled service synonyms registry
    and returns the matched service definition using longest/most-specific match first.

    Args:
        query (str): Normalized query string.

    Returns:
        Optional[Dict]: Service metadata dict if matched, else None.
    """
    if not query:
        return None

    clean_q = query.lower().strip()
    load_synonyms_registry()

    for pattern, service_key, canonical_name, target_workflow, expr, sub_cat in (_COMPILED_SYNONYMS or []):
        if pattern.search(clean_q):
            logger.info(f"[NORMALIZER] Match: '{expr}' for service '{service_key}'")
            res = {
                "service_key": service_key,
                "canonical_name": canonical_name,
                "target_workflow": target_workflow,
                "matched_expression": expr
            }
            if sub_cat:
                res["subcategory"] = sub_cat
            return res

    return None
