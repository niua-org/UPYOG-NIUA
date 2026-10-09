"""
Prompts Package for UPYOG Voice Bot:
- system_prompt: Master domain system prompt, keyword dictionaries, and RAG prompt builders.
- intent_prompts: Multi-category intent classification and dynamic greeting prompts.
- workflow_prompts: Field extraction, conversational asking, filter prompts for Ad Booking and Grievance workflows, and Address parsing.
- memory_prompts: Conversational summarization prompts for long-term memory.
"""

from prompts.system_prompt import (
    SYSTEM_PROMPT,
    OUT_OF_DOMAIN_KEYWORDS,
    UPYOG_KEYWORDS,
    HARD_BLOCK_TOPICS,
    build_rag_system_prompt,
    build_rag_stream_system_prompt,
    build_query_rewriter_prompt,
)

from prompts.intent_prompts import (
    build_intent_classifier_prompt,
    build_greeting_prompt,
)

from prompts.workflow_prompts import (
    build_adv_extract_prompt,
    build_adv_question_prompt,
    build_adv_past_booking_filter_prompt,
    build_grievance_extract_prompt,
    build_grievance_question_prompt,
    build_address_parser_prompt,
)

from prompts.memory_prompts import (
    build_memory_summary_prompt,
)

__all__ = [
    "SYSTEM_PROMPT",
    "OUT_OF_DOMAIN_KEYWORDS",
    "UPYOG_KEYWORDS",
    "HARD_BLOCK_TOPICS",
    "build_rag_system_prompt",
    "build_rag_stream_system_prompt",
    "build_query_rewriter_prompt",
    "build_intent_classifier_prompt",
    "build_greeting_prompt",
    "build_adv_extract_prompt",
    "build_adv_question_prompt",
    "build_adv_past_booking_filter_prompt",
    "build_grievance_extract_prompt",
    "build_grievance_question_prompt",
    "build_address_parser_prompt",
    "build_memory_summary_prompt",
]
