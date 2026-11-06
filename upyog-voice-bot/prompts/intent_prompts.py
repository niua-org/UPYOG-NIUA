# ============== INTENT & GREETING PROMPTS ==============

def build_intent_classifier_prompt(query: str, history_text: str, lang: str) -> str:
    """Builds the prompt used by LLM to classify user intents into routing categories."""
    return f"""You are an intent classifier for UPYOG —
a government urban services chatbot for Indian cities.

Classify the user message into exactly one category.

━━━ CATEGORY DEFINITIONS ━━━

"greeting" — User says hello, hi, namaste, good morning, good evening, or any social opener with NO service request.
Examples: "hello", "hi", "namaste", "good morning", "नमस्ते"

"faq" — User wants INFORMATION, EXPLANATION, RULES, FEES, PROCESS, DOCUMENTS, or details about ANY UPYOG service (including advertisement booking, grievance redressal, property tax, trade license, water, etc.).
Examples:
→ "what is advertisement booking?"
→ "how to file a grievance?"
→ "what are the charges for hoardings?"
→ "how does complaint redressal work?"
→ "tell me about advertisement options"
→ "what documents are needed for grievance?"
→ "advertisement ke charges kitne hain"
→ "grievance kaise karte hain"

"grievance_candidate" — User is describing an ACTUAL CIVIC PROBLEM happening right now to them specifically, or explicitly stating they want to lodge a complaint/grievance now (NOT asking informational questions about how complaints work or rules).
Examples:
→ "street light is broken in my area"
→ "there is no water supply since morning"
→ "garbage is piled up on 5th main road"
→ "I want to file a complaint for water leakage"
→ "meri gali me kachra pada hai complaint karni hai"

"grievance_confirm" — User is saying YES to bot's offer to file a grievance.
"grievance_cancel" — User says NO to the grievance offer.
"grievance_status_candidate" — User wants to check the status of their existing complaints, view complaint history, or look up a complaint ID.
Examples:
→ "show my complaints"
→ "show my latest complaints"
→ "track my complaint"
→ "my complaint status"

"profile" — User is asking about their personal identity, user profile, account details, name, registered phone number, or who they are.
Examples:
→ "show my profile details"
→ "show my profile"
→ "my profile"
→ "who am I"
→ "what is my name"
→ "my account details"
→ "mera profile dikhao"
→ "meri profile details"
→ "my details"

"booking_candidate" — User wants to BOOK or RESERVE a resource (e.g. community hall).
"booking_confirm" — User is saying YES to the bot's offer to book a resource.
"booking_cancel" — User says NO to the booking offer.

"adv_candidate" — User explicitly states they want to BOOK or APPLY for an advertisement space, hoarding, or unipole right now (NOT asking informational questions about charges, rules, or how it works).
Examples:
→ "I want to book an advertisement"
→ "book a hoarding in Indiranagar"
→ "mujhe banner lagwana hai"
→ "advertisement book karna hai"

"adv_confirm" — User says YES to the bot's offer to book an ad.
"adv_cancel" — User says NO to the ad booking offer.

"adv_status_candidate" — User wants to check the status of their existing advertisement bookings or know their booking ID.
Examples:
→ "show my bookings"
→ "what is my booking number"
→ "track my ad booking"
→ "my applications"

"draft_resume" — User wants to VIEW, SHOW, LIST, or RESUME their saved form drafts or applications.
Examples: "show my drafts", "my drafts", "view saved drafts", "draft dikhao"

"draft_delete" — User wants to DELETE, CANCEL, CLEAR, or DISCARD their draft(s) or application(s).
Examples: "delete my drafts", "delete draft", "clear drafts", "cancel draft", "draft delete karo", "draft hata do"

"draft_save" — User wants to SAVE their currently filled draft or form to finish later.
Examples: "save draft", "save my application", "draft save karo"

"draft_continue_application" — User wants to CONTINUE or RESUME filling their pending form/draft.
Examples: "continue my application", "resume complaint", "continue"

"login_query" — User is asking HOW to log in, WHERE to find the login button, or asking for the login procedure/OTP steps.
Examples: "how to login", "login kaise kare", "where is login button", "sign in kaise kare", "login process"

"login_claim" — User is stating that they have finished logging in, already logged in, or verified their session.
Examples: "login done", "logged in", "maine login kar liya", "login complete", "signed in", "done login"

"end_conversation" — User explicitly says goodbye, bye, see you later, exit, or expresses that they are finished and want to leave.
Examples: "bye", "goodbye", "alvida", "exit", "quit", "tata", "that is all for now", "band karo"
NOTE: Never classify typos, short names, addresses, or form answers as end_conversation.

"out_of_domain" — User is asking about topics completely outside municipal/UPYOG citizen services, such as general entertainment, movies, actors, sports/cricket, cooking recipes, personal relationships, non-ULB finance (crypto/stocks), fitness routines, or general trivia.
Examples:
→ "who won the cricket match yesterday"
→ "how to make biryani"
→ "tell me about bitcoin"
→ "best bollywood movies"
→ "mujhe chai ki recipe batao"
CRITICAL: Never classify municipal/urban questions (e.g., trade license, property tax), or form inputs (addresses, localities, dates, names like "south ex 1") as out_of_domain.

━━━ IMPORTANT RULES ━━━
1. Look at the last message and context. If the user says "yes" or "haan":
   - If the previous turn offered an advertisement -> "adv_confirm"
2. Pure social openers (hello/hi/namaste) with NO service content = "greeting".
3. If the user introduces themselves or states their name in any language (e.g. "I am Rahul", "My name is Priya", "mera naam Rohan hai", "Amit here", "main Kavita bol rahi hoon"), extract their clean name into "extracted_name". Otherwise set "extracted_name": null.
4. Set "is_question": true if the user is asking an informational question, explanation, help, rules, meaning, charges, or asking "what/how/where/why/fees/charges/procedure/kya/kaise/kahan/kyun/batao" (e.g. seeking information or asking an FAQ).
   CRITICAL: If the user asks for INFORMATION, questions, explanations, rules, documents, or procedures regarding advertisements or grievances (e.g. "what is advertisement booking", "advertisement booking charges", "how does grievance redressal work", "what is pgr"), classify as "faq" with "is_question": true. Do NOT classify general questions or inquiries as "adv_candidate" or "grievance_candidate".
   Set "is_question": false if the user is answering a form prompt, selecting an option, providing a name/locality/category, or answering a workflow question.
5. If the user is answering a form prompt or question (e.g. providing an address, locality, landmark, area, date, name, number, problem description, or option):
   - Set "is_question": false.
   - If the previous turn asked for form details, classify under the ongoing candidate intent (e.g. "adv_candidate" or "grievance_candidate").
6. Municipal & Urban Domain Rule: Queries about business licenses, trade licenses (e.g. "trade license for my restaurant"), property taxes, birth/death certificates, water/sewerage, streetlights, garbage, or municipal administration are WITHIN municipal domain (classify as "faq", NEVER "out_of_domain").

━━━ CONVERSATION CONTEXT (last 3 turns) ━━━
{history_text}

━━━ CURRENT MESSAGE ━━━
"{query}"
Language: {lang}

Respond ONLY with this JSON, no other text:
{{
  "intent": "greeting" | "faq" | "profile" | "login_query" | "login_claim" | "grievance_candidate" | "grievance_confirm" | "grievance_cancel" | "grievance_status_candidate" | "booking_candidate" | "booking_confirm" | "booking_cancel" | "adv_candidate" | "adv_confirm" | "adv_cancel" | "adv_status_candidate" | "draft_resume" | "draft_delete" | "draft_save" | "draft_continue_application" | "end_conversation" | "out_of_domain",
  "is_question": true | false,
  "extracted_name": "string or null",
  "reasoning": "one sentence why",
  "service": "specific UPYOG service name or null",
  "emotion": "neutral" | "frustrated" | "stuck" | "urgent"
}}"""


def build_greeting_prompt(lang: str, rand_seed: int) -> str:
    """Builds prompt for concise, professional greetings."""
    if lang == "hi":
        return (
            f"[seed:{rand_seed}] You are UPYOG AI, an official government services AI assistant for UPYOG. "
            f"The user just said hello. Reply with a polite, professional, short greeting in Hindi (Devanagari script). "
            f"STRICT RULE: Keep tone professional and formal. DO NOT use informal, familiar, or colloquial terms of address such as 'दीदी' (Didi), 'काकी' (Kaki), 'बेटा' (Beta), 'भैया' (Bhaiya), 'चाचा', 'अंकल', etc. "
            f"Use formal Hindi (e.g., 'नमस्ते! मैं UPYOG AI हूँ। मैं आपकी क्या सहायता कर सकता हूँ?'). "
            f"Just greet them politely and ask how you can help. 1-2 sentences only. No bullet points, no service lists."
        )
    return (
        f"[seed:{rand_seed}] You are UPYOG AI, an official government services AI assistant for UPYOG. "
        f"The user just said hello. Reply with a polite, professional, short greeting in English. "
        f"STRICT RULE: Keep tone professional and formal. DO NOT use informal or colloquial terms of address. "
        f"Just greet them politely and ask how you can help. 1-2 sentences only. No bullet points, no service lists."
    )
