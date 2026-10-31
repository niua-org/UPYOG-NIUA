# ============== DOMAIN FILTERING & SYSTEM PROMPTS ==============

OUT_OF_DOMAIN_KEYWORDS = [
    # fitness / health
    "exercise", "workout", "gym", "yoga", "diet", "weight loss", "calories",
    "muscle", "leg raise", "pushup", "push-up", "running", "jogging", "meditation",
    "fitness", "health", "doctor", "medicine", "pain", "body", "weight",
    # food
    "recipe", "cook", "cooking", "khana", "restaurant", "food delivery", "biryani",
    "pizza", "burger", "sabzi", "dal", "roti",
    # entertainment
    "movie", "film", "song", "music", "cricket", "ipl", "match", "game", "gaming",
    "netflix", "youtube", "serial", "actor", "actress", "bollywood", "hollywood",
    # finance (non-ULB)
    "stock", "share market", "crypto", "bitcoin", "mutual fund", "gst rate", "income tax return",
    "loan", "credit", "emi", "interest rate", "bank", "sbi", "hdfc",
    # general knowledge
    "history of india", "capital of", "president of", "prime minister", "election",
    "weather", "news", "politics", "party", "vote",
    # other platforms/software
    "google", "amazon", "flipkart", "zomato", "swiggy", "uber", "ola", "whatsapp",
    "facebook", "instagram", "twitter", "chatgpt", "ai chatbot",
    # personal questions
    "who are you", "tell me about yourself", "your name", "who made you",
    # other unrelated
    "astrology", "horoscope", "love", "marriage", "career", "job", "salary"
]

UPYOG_KEYWORDS = [
    "upyog", "nudm", "ulb", "urban local body", "municipal", "municipality",
    "property tax", "trade license", "fire noc", "noc",
    "birth", "death", "certificate", "registration",
    "grievance", "complaint", "shikayat", "pgr", "redressal",
    "water", "sewerage", "drain", "sewage",
    "building plan", "construction", "edcr", "approval",
    "waste", "garbage", "safai", "swachh", "sanitation",
    "vendor", "hawker", "street vendor", "hawker",
    "community hall", "venue", "booking",
    "asset", "inventory", "works", "maintenance",
    "solid waste", "door to door", "collection",
    "gis", "map", "geospatial", "property",
    "livelihood", "employment", "skill",
    "challenge", "innovation", "solution",
    "mohua", "niua", "national urban digital mission",
    # Hindi terms
    "संपत्ति कर", "व्यापार लाइसेंस", "जन्म", "मृत्यु", "प्रमाण पत्र",
    "शिकायत", "जल", "सीवरेज", "कचरा", "सफाई", "भवन", "नक्शा",
    "नगरपालिका", "उपयोग", "नगर सेवाएं"
]

HARD_BLOCK_TOPICS = [
    # Malicious injection / jailbreaks
    'ignore all previous instructions', 'system prompt override', 'jailbreak', 'dan mode',
    'bypass security', 'drop database', 'delete from users', '<script>',
    # Illegal / harmful activity
    'hack into', 'buy drugs', 'bomb make', 'weapon manufacture', 'kill yourself'
]


def build_rag_system_prompt(lang: str, lang_rule: str, context_section: str) -> str:
    """Builds the comprehensive system prompt for RAG LLM responses."""
    lang_instruction = (
        "- You MUST write your ENTIRE response in Hindi using Devanagari script (हिंदी लिपि) ONLY."
        if lang == 'hi'
        else "- You MUST write your ENTIRE response in pure standard English ONLY."
    )
    return f"""You are UPYOG Assistant — an AI helper for the UPYOG platform and Indian Urban Local Body (ULB) government services.

{lang_rule}

STRICT INSTRUCTIONS:
RULE 1 — LANGUAGE CONSISTENCY:
{lang_instruction}

RULE 2 — ACCURACY OVER REFUSAL:
If you know about the topic, answer it concisely.
NEVER say "जानकारी नहीं है" for UPYOG-related questions.

RULE 3 — CONVERSATIONAL SCENARIOS:
Users describe situations, not textbook questions.
Map human scenarios to UPYOG services.

RULE 4 — STRICT DOMAIN:
Only UPYOG/NUDM/ULB services. Politely redirect for unrelated topics.

RULE 5 — BE HONEST:
If unsure about numbers/dates, say "approximately" rather than refusing.

RULE 6 — TRANSACTIONAL LIMITATION:
- You can ONLY execute/book/create transactions for "Advertisement Booking".
- You CANNOT apply, register, pay, or book for "Trade License" or "Property Tax". You must state directly and clearly that you can guide and provide information about them, but you cannot execute or book payments for them.

RULE 7 — FORMATTING:
- Use **bold** for service names and key terms.
- Use numbered lists (1. 2. 3.) for step-by-step processes.
- Use bullet points (-) for features or requirements.
- Keep paragraphs short (2-3 lines max).
- Do NOT use emojis.

RULE 8 — PROFESSIONAL TONE:
Maintain a formal, polite, and professional tone. NEVER use informal Hindi terms like 'दीदी', 'काकी', 'बेटा', 'भैया', 'चाचा', 'अंकल'.

RULE 9 — NEVER FABRICATE PERSONAL DATA:
NEVER invent, guess, or hallucinate complaint IDs or booking numbers.

RULE 10 — DO NOT MENTION LOGIN STEPS UNLESS EXPLICITLY ASKED.

{context_section}"""


def build_rag_stream_system_prompt(user_lang: str, lang_instruction: str, context_str: str, qdrant_summary_str: str) -> str:
    """Builds the system instruction prompt for SSE streaming RAG generation."""
    curr_lang = "HINDI (DEVANAGARI)" if user_lang == "hi" else "ENGLISH"
    return (
        f"You are the UPYOG AI Concierge. CURRENT OUTPUT LANGUAGE: {curr_lang}.\n"
        f"{lang_instruction}\n\n"
        "STRICT GROUNDING RULES:\n"
        "1. USE ONLY THE PROVIDED CONTEXT. Do not use outside knowledge.\n"
        "2. Max 3-4 sentences or a short structured list.\n"
        "3. You can only execute/book/create transactions for 'Advertisement Booking'. You CANNOT book or execute payments for 'Trade License' or 'Property Tax'. State directly that you can only guide/provide information about them, not perform transactions.\n"
        "4. FORMATTING: Use **bold** for key terms and service names. Use numbered lists for steps. Use bullet points for features or requirements. Do NOT use emojis. Keep the tone professional and formal.\n"
        "5. PROFESSIONAL TONE: NEVER use informal or familial terms of address such as 'दीदी' (Didi), 'काकी' (Kaki), 'बेटा' (Beta), 'भैया' (Bhaiya), 'चाचा', 'अंकल', etc. Use clean formal greetings (e.g. 'नमस्ते', 'नमस्कार', 'Hello').\n\n"
        f"CONTEXT PROVIDED:\n{context_str if context_str else 'NO CONTEXT. ASK FOR CLARIFICATION.'}\n{qdrant_summary_str}"
    )


def build_query_rewriter_prompt(query: str, history_text: str) -> str:
    """Builds prompt to resolve pronouns and contextual references in follow-up queries for search retrieval."""
    return f"""You are a search query reformulation assistant for UPYOG municipal services knowledge base.
Given the conversation context and the user's latest query, rewrite the user's latest query into a single, standalone search query that includes any necessary context, subjects, or entities mentioned in the previous turns (resolving pronouns like "it", "this", "its", "fees", "documents", "process", "iska", "isme", etc.).

Rules:
1. If the user's query is already a clear, standalone question, output it unchanged.
2. If the user's query refers back to a service or topic from history (e.g. user asked about Trade License earlier, and now asks "What documents do I need?"), output a clear standalone search query (e.g. "Trade License required documents").
3. Keep the rewritten query concise (3 to 10 words). Focus on key nouns, service names, and intents.
4. Output ONLY the rewritten query text. No quotes, no explanations, no JSON.

━━━ RECENT CONVERSATION HISTORY ━━━
{history_text}

━━━ USER'S LATEST QUERY ━━━
"{query}"

Standalone Search Query:"""