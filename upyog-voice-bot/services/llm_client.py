import os
import re
import time
import logging
from typing import List, Dict, Any, Optional, Iterator, Generator
from dotenv import load_dotenv

# Ensure environment variables are loaded
load_dotenv()

logger = logging.getLogger(__name__)

# Try importing OpenAI client for AWS Bedrock API
try:
    from openai import OpenAI
except ImportError:
    OpenAI = None

# Try importing Groq client as fallback
try:
    from groq import Groq
except ImportError:
    Groq = None

# Try importing LangChain BaseChatModel for workflow integration
try:
    from langchain_core.language_models.chat_models import BaseChatModel
    from langchain_core.messages import BaseMessage, AIMessage, SystemMessage, HumanMessage
    from langchain_core.outputs import ChatResult, ChatGeneration
except ImportError:
    BaseChatModel = None
    BaseMessage = None
    AIMessage = None
    SystemMessage = None
    HumanMessage = None
    ChatResult = None
    ChatGeneration = None


# Configuration Defaults
AWS_BEARER_TOKEN_BEDROCK = os.environ.get("AWS_BEARER_TOKEN_BEDROCK")
BEDROCK_REGION = os.environ.get("BEDROCK_REGION", "ap-south-1")
BEDROCK_MODEL_ID = os.environ.get("BEDROCK_MODEL_ID", "openai.gpt-oss-120b-1:0")

GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
GROQ_MODEL = os.environ.get("GROQ_MODEL", "openai/gpt-oss-20b")


def clean_reasoning_tags(text: str) -> str:
    """
    Strips reasoning and thinking blocks emitted by open-weight reasoning models
    such as openai.gpt-oss-120b or deepseek models (<reasoning>...</reasoning>, <think>...</think>).
    Ensures that TTS synthesis and downstream parsers receive only direct assistant responses.
    """
    if not text:
        return ""

    # Clean closed tags
    cleaned = re.sub(r"<reasoning>[\s\S]*?</reasoning>", "", text)
    cleaned = re.sub(r"<think>[\s\S]*?</think>", "", cleaned)

    # Clean unclosed tags if truncated mid-thought
    if "<reasoning>" in cleaned and "</reasoning>" not in cleaned:
        cleaned = re.sub(r"<reasoning>[\s\S]*", "", cleaned)
    if "<think>" in cleaned and "</think>" not in cleaned:
        cleaned = re.sub(r"<think>[\s\S]*", "", cleaned)

    return cleaned.strip()


class ReasoningStreamFilter:
    """
    Streaming filter to prevent reasoning tokens (<reasoning>...</reasoning> or <think>...</think>)
    from being pushed to TTS generators or front-end stream subscribers in real-time.
    """

    def __init__(self):
        self.in_reasoning = False
        self.buffer = ""
        self.tag_checked = False

    def process(self, chunk: str) -> str:
        if not chunk:
            return ""
        self.buffer += chunk

        if not self.tag_checked:
            stripped = self.buffer.lstrip()
            if not stripped:
                return ""
            if not stripped.startswith("<"):
                self.tag_checked = True
                res = self.buffer
                self.buffer = ""
                return res
            if "<reasoning>" in self.buffer:
                self.in_reasoning = True
                self.tag_checked = True
                self.buffer = self.buffer.split("<reasoning>", 1)[1]
            elif "<think>" in self.buffer:
                self.in_reasoning = True
                self.tag_checked = True
                self.buffer = self.buffer.split("<think>", 1)[1]
            elif len(stripped) > 15:
                # Longer than '<reasoning>' without match -> treat as normal content
                self.tag_checked = True
                res = self.buffer
                self.buffer = ""
                return res
            else:
                # Still checking tag prefix
                return ""

        if self.in_reasoning:
            if "</reasoning>" in self.buffer:
                self.in_reasoning = False
                res = self.buffer.split("</reasoning>", 1)[1]
                self.buffer = ""
                return res
            elif "</think>" in self.buffer:
                self.in_reasoning = False
                res = self.buffer.split("</think>", 1)[1]
                self.buffer = ""
                return res
            return ""
        else:
            res = self.buffer
            self.buffer = ""
            return res


class StreamDelta:
    def __init__(self, content: str):
        self.content = content


class StreamChoice:
    def __init__(self, delta: StreamDelta):
        self.delta = delta


class StreamChunk:
    def __init__(self, content: str):
        self.choices = [StreamChoice(StreamDelta(content))]


class ChatCompletionsProxy:
    """
    Implements openai/groq compatible .create() interface with Bedrock as primary
    and Groq as seamless fallback.
    """

    def __init__(self, provider: "UnifiedLLMClient"):
        self.provider = provider

    def create(
        self,
        messages: List[Dict[str, Any]],
        model: Optional[str] = None,
        temperature: float = 0.1,
        max_tokens: Optional[int] = None,
        stream: bool = False,
        **kwargs: Any
    ):
        return self.provider.create_completion(
            messages=messages,
            model=model,
            temperature=temperature,
            max_tokens=max_tokens,
            stream=stream,
            **kwargs
        )


class ChatProxy:
    def __init__(self, provider: "UnifiedLLMClient"):
        self.completions = ChatCompletionsProxy(provider)


class UnifiedLLMClient:
    """
    Unified LLM Client providing resilient routing:
    - Primary: AWS Bedrock API Key / Bearer token via Bedrock Runtime OpenAI-compatible endpoint
    - Fallback: Groq API Key
    - Automatic failover on connection error, quota exhaustion, 429, or server errors
    - Automatic stripping of reasoning tags (<reasoning>...</reasoning>)
    """

    def __init__(self):
        self.chat = ChatProxy(self)
        self._bedrock_client: Optional[OpenAI] = None
        self._groq_client: Optional[Groq] = None

    def _get_bedrock_client(self) -> Optional[OpenAI]:
        token = os.environ.get("AWS_BEARER_TOKEN_BEDROCK", AWS_BEARER_TOKEN_BEDROCK)
        region = os.environ.get("BEDROCK_REGION", BEDROCK_REGION)

        if not token or not OpenAI:
            return None

        if self._bedrock_client is None:
            base_url = f"https://bedrock-runtime.{region}.amazonaws.com/openai/v1"
            self._bedrock_client = OpenAI(
                base_url=base_url,
                api_key=token,
                timeout=25.0
            )
        return self._bedrock_client

    def _get_groq_client(self) -> Optional[Groq]:
        api_key = os.environ.get("GROQ_API_KEY", GROQ_API_KEY)
        if not api_key or not Groq:
            return None

        if self._groq_client is None:
            self._groq_client = Groq(
                api_key=api_key,
                timeout=25.0
            )
        return self._groq_client

    def is_configured(self) -> bool:
        return bool(self._get_bedrock_client() or self._get_groq_client())

    def create_completion(
        self,
        messages: List[Dict[str, Any]],
        model: Optional[str] = None,
        temperature: float = 0.1,
        max_tokens: Optional[int] = None,
        stream: bool = False,
        **kwargs: Any
    ):
        bedrock_client = self._get_bedrock_client()
        bedrock_model = os.environ.get("BEDROCK_MODEL_ID", BEDROCK_MODEL_ID)
        groq_model = os.environ.get("GROQ_MODEL", GROQ_MODEL)

        # -------------------------------------------------------------
        # 1. ATTEMPT PRIMARY: AWS BEDROCK
        # -------------------------------------------------------------
        if bedrock_client:
            try:
                # Reasoning models generate thinking tokens that consume max_tokens budget.
                # Provide token headroom for Bedrock so answer is not truncated mid-reasoning.
                bedrock_max_tokens = max((max_tokens or 400) + 400, 750) if max_tokens else None

                target_model = bedrock_model
                logger.info(f"[LLM Router] Calling AWS Bedrock (model: {target_model}, region: {os.environ.get('BEDROCK_REGION', BEDROCK_REGION)})")
                t0 = time.time()

                if stream:
                    response_stream = bedrock_client.chat.completions.create(
                        model=target_model,
                        messages=messages,
                        temperature=temperature,
                        max_tokens=bedrock_max_tokens,
                        stream=True,
                        **kwargs
                    )
                    return self._wrap_bedrock_stream(response_stream)
                else:
                    response = bedrock_client.chat.completions.create(
                        model=target_model,
                        messages=messages,
                        temperature=temperature,
                        max_tokens=bedrock_max_tokens,
                        stream=False,
                        **kwargs
                    )
                    raw_content = response.choices[0].message.content or ""
                    cleaned = clean_reasoning_tags(raw_content)

                    # If model truncated inside reasoning and returned empty answer, failover to Groq
                    if not cleaned.strip() and raw_content.strip():
                        raise RuntimeError(f"Bedrock response truncated during reasoning (raw len: {len(raw_content)})")

                    response.choices[0].message.content = cleaned
                    elapsed = time.time() - t0
                    logger.info(f"[LLM Router] AWS Bedrock succeeded in {elapsed:.2f}s (response len: {len(cleaned)} chars)")
                    return response

            except Exception as e:
                logger.warning(
                    f"[LLM Failover] AWS Bedrock call failed: {e!r}. Seamlessly falling back to Groq...",
                    exc_info=False
                )

        # -------------------------------------------------------------
        # 2. ATTEMPT FALLBACK: GROQ
        # -------------------------------------------------------------
        groq_client = self._get_groq_client()
        if groq_client:
            try:
                target_model = groq_model
                logger.info(f"[LLM Router] Calling fallback Groq (model: {target_model})")
                t0 = time.time()

                response = groq_client.chat.completions.create(
                    model=target_model,
                    messages=messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    stream=stream,
                    **kwargs
                )

                if stream:
                    return response

                raw_content = response.choices[0].message.content or ""
                response.choices[0].message.content = clean_reasoning_tags(raw_content)
                elapsed = time.time() - t0
                logger.info(f"[LLM Router] Groq fallback succeeded in {elapsed:.2f}s")
                return response

            except Exception as e:
                logger.error(f"[LLM Router] Groq fallback also failed: {e}", exc_info=True)
                raise e

        raise RuntimeError("No LLM provider configured (neither AWS Bedrock nor Groq is available).")

    def _wrap_bedrock_stream(self, stream_iterator: Iterator[Any]) -> Generator[StreamChunk, None, None]:
        """
        Filters out <reasoning> blocks from Bedrock stream and yields StreamChunk instances.
        """
        filter_inst = ReasoningStreamFilter()
        for chunk in stream_iterator:
            delta_content = ""
            if chunk.choices and chunk.choices[0].delta and chunk.choices[0].delta.content:
                delta_content = chunk.choices[0].delta.content

            filtered = filter_inst.process(delta_content)
            if filtered:
                yield StreamChunk(filtered)


# Singleton instance
_unified_client: Optional[UnifiedLLMClient] = None

def get_llm_client() -> UnifiedLLMClient:
    global _unified_client
    if _unified_client is None:
        _unified_client = UnifiedLLMClient()
    return _unified_client


# ==============================================================================
# LangChain BaseChatModel implementation for workflows and graph states
# ==============================================================================

if BaseChatModel is not None:
    class UnifiedChatModel(BaseChatModel):
        """
        LangChain-compatible chat model wrapping AWS Bedrock with Groq fallback.
        Directly drop-in replacement for ChatGroq in workflow/grievance.py,
        workflow/adv_booking.py, and clients/upyog_client.py.
        """

        model_name: str = "unified-bedrock-groq"
        temperature: float = 0.0

        def _generate(
            self,
            messages: List[BaseMessage],
            stop: Optional[List[str]] = None,
            run_manager: Optional[Any] = None,
            **kwargs: Any,
        ) -> ChatResult:
            formatted_messages = []
            for msg in messages:
                if isinstance(msg, SystemMessage):
                    role = "system"
                elif isinstance(msg, HumanMessage):
                    role = "user"
                elif isinstance(msg, AIMessage):
                    role = "assistant"
                elif hasattr(msg, "type") and msg.type:
                    role = "user" if msg.type == "human" else ("assistant" if msg.type == "ai" else "system")
                else:
                    role = "user"
                formatted_messages.append({"role": role, "content": str(msg.content)})

            client = get_llm_client()
            resp = client.create_completion(
                messages=formatted_messages,
                temperature=self.temperature,
                max_tokens=kwargs.get("max_tokens", 500)
            )

            content = resp.choices[0].message.content or ""
            return ChatResult(
                generations=[ChatGeneration(message=AIMessage(content=content))]
            )

        @property
        def _llm_type(self) -> str:
            return "unified_bedrock_groq"

else:
    class UnifiedChatModel:
        def __init__(self, *args, **kwargs):
            pass
        def invoke(self, *args, **kwargs):
            raise NotImplementedError("langchain_core not installed")


def get_unified_chat_model(temperature: float = 0.0) -> Optional[UnifiedChatModel]:
    """
    Returns an initialized LangChain chat model backed by Bedrock with Groq fallback,
    or None if no credentials are configured.
    """
    client = get_llm_client()
    if not client.is_configured():
        return None
    return UnifiedChatModel(temperature=temperature)

