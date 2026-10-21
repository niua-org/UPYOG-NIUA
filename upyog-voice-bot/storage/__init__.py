"""
Storage package for UPYOG Voice Bot:
- redis_manager: Resilient Redis client, citizen chat histories, draft state, token verification.
- qdrant_manager: Qdrant client, MemoryManager for long-term vector interactions and isolated module drafts.
"""

from storage.redis_manager import (
    r_client,
    ResilientRedisClient,
    verify_connection,
    close_connection,
    get_short_term_memory,
    save_short_term_memory,
    clear_short_term_memory,
    save_long_term_memory,
    get_long_term_memory,
    get_any_user_profile_name,
    save_user_profile_name,
    save_chat_history,
    get_chat_history,
    get_user_from_redis_token,
)

from storage.qdrant_manager import (
    MemoryManager,
    shared_memory,
    init_collections,
    client as qdrant_client,
    LONG_TERM_MEMORY_COLLECTION,
    KNOWLEDGE_BASE_COLLECTION,
    DRAFT_STATE_COLLECTION,
)

__all__ = [
    "r_client",
    "ResilientRedisClient",
    "verify_connection",
    "close_connection",
    "get_short_term_memory",
    "save_short_term_memory",
    "clear_short_term_memory",
    "save_long_term_memory",
    "get_long_term_memory",
    "get_any_user_profile_name",
    "save_user_profile_name",
    "save_chat_history",
    "get_chat_history",
    "get_user_from_redis_token",
    "MemoryManager",
    "shared_memory",
    "init_collections",
    "qdrant_client",
    "LONG_TERM_MEMORY_COLLECTION",
    "KNOWLEDGE_BASE_COLLECTION",
    "DRAFT_STATE_COLLECTION",
]
