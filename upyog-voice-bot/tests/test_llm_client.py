import os
import unittest
from unittest.mock import patch, MagicMock
from services.llm_client import (
    clean_reasoning_tags,
    ReasoningStreamFilter,
    UnifiedLLMClient,
    get_llm_client,
    get_unified_chat_model,
    StreamChunk
)
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage


class TestLLMClient(unittest.TestCase):

    def test_clean_reasoning_tags(self):
        # 1. Closed reasoning tags
        raw = "<reasoning>Thinking process here...</reasoning>Final answer to citizen."
        self.assertEqual(clean_reasoning_tags(raw), "Final answer to citizen.")

        # 2. Closed think tags
        raw_think = "<think>Some internal thought</think>Direct output"
        self.assertEqual(clean_reasoning_tags(raw_think), "Direct output")

        # 3. Multiline reasoning tags
        raw_multiline = "<reasoning>\nStep 1: Check intent\nStep 2: Generate response\n</reasoning>\n\nNamaste!"
        self.assertEqual(clean_reasoning_tags(raw_multiline), "Namaste!")

        # 4. Truncated / unclosed reasoning tags
        raw_unclosed = "<reasoning>The user is asking about water complaint but..."
        self.assertEqual(clean_reasoning_tags(raw_unclosed), "")

        # 5. Clean text without reasoning
        clean = "This is normal text."
        self.assertEqual(clean_reasoning_tags(clean), "This is normal text.")

    def test_reasoning_stream_filter(self):
        filt = ReasoningStreamFilter()
        chunks = [
            "<reasoning>",
            "Thinking about",
            " user query",
            "</reasoning>",
            "Hello",
            " world!"
        ]
        emitted = []
        for c in chunks:
            out = filt.process(c)
            if out:
                emitted.append(out)

        self.assertEqual("".join(emitted).strip(), "Hello world!")

    def test_reasoning_stream_filter_no_tags(self):
        filt = ReasoningStreamFilter()
        chunks = ["Hello ", "how ", "can ", "I help?"]
        emitted = [filt.process(c) for c in chunks if filt.process(c)]
        self.assertEqual("".join(emitted), "Hello how can I help?")

    @patch("services.llm_client.OpenAI")
    @patch("services.llm_client.Groq")
    def test_bedrock_primary_success(self, mock_groq_cls, mock_openai_cls):
        mock_openai_inst = MagicMock()
        mock_openai_cls.return_value = mock_openai_inst

        # Simulate Bedrock response with reasoning tag
        mock_choice = MagicMock()
        mock_choice.message.content = "<reasoning>Let me think</reasoning>Bedrock response"
        mock_resp = MagicMock()
        mock_resp.choices = [mock_choice]
        mock_openai_inst.chat.completions.create.return_value = mock_resp

        client = UnifiedLLMClient()
        client._bedrock_client = mock_openai_inst
        res = client.chat.completions.create(
            messages=[{"role": "user", "content": "hi"}],
            max_tokens=100
        )

        self.assertEqual(res.choices[0].message.content, "Bedrock response")
        # Ensure Groq was not called
        mock_groq_cls.return_value.chat.completions.create.assert_not_called()

    @patch("services.llm_client.OpenAI")
    @patch("services.llm_client.Groq")
    def test_bedrock_failover_to_groq(self, mock_groq_cls, mock_openai_cls):
        mock_openai_inst = MagicMock()
        mock_openai_inst.chat.completions.create.side_effect = Exception("Bedrock 503 Service Unavailable")

        mock_groq_inst = MagicMock()
        mock_choice = MagicMock()
        mock_choice.message.content = "Groq fallback response"
        mock_resp = MagicMock()
        mock_resp.choices = [mock_choice]
        mock_groq_inst.chat.completions.create.return_value = mock_resp

        client = UnifiedLLMClient()
        client._bedrock_client = mock_openai_inst
        client._groq_client = mock_groq_inst

        res = client.chat.completions.create(
            messages=[{"role": "user", "content": "hi"}],
            max_tokens=100
        )

        self.assertEqual(res.choices[0].message.content, "Groq fallback response")
        mock_openai_inst.chat.completions.create.assert_called_once()
        mock_groq_inst.chat.completions.create.assert_called_once()

    def test_langchain_unified_chat_model(self):
        model = get_unified_chat_model(temperature=0.0)
        self.assertIsNotNone(model)

        with patch("services.llm_client.UnifiedLLMClient.create_completion") as mock_create:
            mock_choice = MagicMock()
            mock_choice.message.content = "Workflow response"
            mock_resp = MagicMock()
            mock_resp.choices = [mock_choice]
            mock_create.return_value = mock_resp

            res = model.invoke([
                SystemMessage(content="System instruction"),
                HumanMessage(content="User input")
            ])

            self.assertIsInstance(res, AIMessage)
            self.assertEqual(res.content, "Workflow response")
            # Verify messages were converted properly
            call_kwargs = mock_create.call_args[1]
            self.assertEqual(call_kwargs["messages"], [
                {"role": "system", "content": "System instruction"},
                {"role": "user", "content": "User input"}
            ])


if __name__ == "__main__":
    unittest.main()

