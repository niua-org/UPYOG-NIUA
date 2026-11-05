# ============== MEMORY SUMMARIZATION PROMPTS ==============

def build_memory_summary_prompt(convo_text: str) -> str:
    """Builds prompt for summarizing conversational chunks into long-term vector memory points."""
    return f"""You are an AI memory summarization assistant.
Please summarize the following conversation chunk into 2 concise sentences. Focus on the core intent, entities, and any factual details discussed.

Conversation:
{convo_text}

Summary:"""
