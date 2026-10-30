"""
Client the UPYOG voice agent uses to call upyog-mcp-server.

The bot does not build RequestInfo or choose a downstream URL. It sends the
citizen session token in the auth-token header and calls the fixed MCP tools.
"""

import json
import logging
import os
import uuid
from typing import Any, Dict, Optional

import requests

logger = logging.getLogger(__name__)


class McpCallError(Exception):
    """The MCP server answered with a business error or an unreadable body."""

    def __init__(self, code: str, message: str, retryable: bool = False):
        super().__init__(message)
        self.code = code
        self.retryable = retryable


class McpUnavailable(Exception):
    """The MCP server could not be reached."""


class UpyogMcpClient:
    """Small Streamable HTTP client for the Java UPYOG MCP server."""

    def __init__(self, url: Optional[str] = None):
        # Production: chatbot → API gateway → MCP (/upyog-mcp-server/mcp). Override with UPYOG_MCP_URL.
        self.url = (
            url
            or os.environ.get("UPYOG_MCP_URL")
            or os.environ.get("UPYOG_GATEWAY_BASE_URL", "http://localhost:8080").rstrip("/") + "/upyog-mcp-server/mcp"
        ).rstrip("/")

    def call_tool(self, name: str, arguments: Dict[str, Any], auth_token: str) -> Dict[str, Any]:
        """Open a short MCP session and call one tool with the citizen token."""
        if not auth_token:
            raise McpCallError("AUTHENTICATION_REQUIRED", "An auth-token is required.", False)
        # Each tool call opens its own session so a dropped connection cannot reuse another citizen's token.
        session_id = self._initialize(auth_token)
        result = self._rpc("tools/call", {"name": name, "arguments": arguments or {}}, auth_token, session_id, request_id=2)
        parsed = _tool_payload(result)
        # prepare_action and search also contain a "code" field in some success shapes.
        # Treat the body as an error only when it is the error contract and not a tool result.
        if isinstance(parsed, dict) and parsed.get("code") and "confirmationToken" not in parsed and "services" not in parsed and "items" not in parsed and "hasPendingBill" not in parsed and "data" not in parsed:
            raise McpCallError(
                str(parsed.get("code")),
                str(parsed.get("message") or parsed.get("code")),
                bool(parsed.get("retryable")),
            )
        return parsed

    def _initialize(self, auth_token: str) -> Optional[str]:
        session_id, body = self._post({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {"name": "upyog-voice-bot", "version": "1.0.0"},
            },
        }, auth_token, None)
        if body.get("error"):
            raise McpCallError("MCP_INIT_FAILED", str(body["error"]), False)
        # The initialized notification has no id. A missing session id is still usable for a stateless server.
        self._post({
            "jsonrpc": "2.0",
            "method": "notifications/initialized",
        }, auth_token, session_id)
        return session_id

    def _rpc(self, method: str, params: Dict[str, Any], auth_token: str, session_id: Optional[str], request_id: int) -> Dict[str, Any]:
        _, body = self._post({
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method,
            "params": params,
        }, auth_token, session_id)
        if body.get("error"):
            message = body["error"].get("message") if isinstance(body["error"], dict) else str(body["error"])
            raise McpCallError("MCP_ERROR", message or "MCP call failed", False)
        return body.get("result") or {}

    def _post(self, payload: Dict[str, Any], auth_token: str, session_id: Optional[str]):
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
            # Gateway validates Auth-Token on /upyog-mcp-server/mcp; MCP forwards it on downstream calls.
            "Auth-Token": auth_token,
            "auth-token": auth_token,
            "x-correlation-id": str(uuid.uuid4()),
        }
        if session_id:
            headers["Mcp-Session-Id"] = session_id
        try:
            response = requests.post(self.url, json=payload, headers=headers, timeout=30)
        except requests.RequestException as exc:
            raise McpUnavailable(f"UPYOG MCP server is unreachable at {self.url}") from exc
        if response.status_code >= 400:
            raise McpCallError("MCP_HTTP_" + str(response.status_code), "The UPYOG assistant service rejected the call.", False)
        new_session = response.headers.get("Mcp-Session-Id") or session_id
        return new_session, _decode_body(response)


def _decode_body(response: requests.Response) -> Dict[str, Any]:
    """Read either a JSON body or one SSE data event from Streamable HTTP."""
    content_type = response.headers.get("Content-Type", "")
    text = response.text or ""
    if "text/event-stream" in content_type or text.startswith("event:") or text.startswith("data:"):
        for line in text.splitlines():
            if line.startswith("data:"):
                data = line[5:].strip()
                if data and data != "[DONE]":
                    return json.loads(data)
        return {}
    if not text.strip():
        return {}
    return response.json()


def _tool_payload(result: Dict[str, Any]) -> Dict[str, Any]:
    structured = result.get("structuredContent")
    if isinstance(structured, dict):
        return structured
    content = result.get("content") or []
    for block in content:
        if not isinstance(block, dict):
            continue
        if block.get("type") == "text" and block.get("text"):
            text = block["text"]
            try:
                parsed = json.loads(text)
            except json.JSONDecodeError:
                return {"message": text}
            return parsed if isinstance(parsed, dict) else {"data": parsed}
    return {}
