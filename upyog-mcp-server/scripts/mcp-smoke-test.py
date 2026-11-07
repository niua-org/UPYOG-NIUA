#!/usr/bin/env python3
"""
UPYOG MCP smoke test: handshake (initialize + Mcp-Session-Id), then all nine tools.

Logs every request/response to stdout and to logs/mcp-smoke-test.log (overwritten each run).

Environment (required):
  MCP_AUTH_TOKEN     Citizen access token
  MCP_USER_UUID      Citizen uuid (x-user-uuid)

Environment (optional):
  MCP_URL            Default http://localhost:8088/mcp
  MCP_CORRELATION_ID Default mcp-smoke-<timestamp>
  MCP_LOG_FILE       Default logs/mcp-smoke-test.log (truncated each run)
  MCP_TENANT         Default pg
  MCP_MOBILE         For PGR search (optional)
  MCP_PROPERTY_ID    For property / billing tools (skip if unset)
  MCP_PROPERTY_TENANT  Default MCP_TENANT or pg.citya
  MCP_PGR_SERVICE_CODE  For prepare_action skeleton (default Streetlight)
  MCP_RUN_PREPARE    true to run prepare_action (no confirm)
  MCP_RUN_CONFIRM    true to run confirm_action (needs MCP_RUN_PREPARE + REDIS on server)

Usage:
  export MCP_AUTH_TOKEN=... MCP_USER_UUID=...
  python3 scripts/mcp-smoke-test.py
"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

REQ_ID = 0


def next_id() -> int:
    global REQ_ID
    REQ_ID += 1
    return REQ_ID


class Logger:
    def __init__(self, path: Path) -> None:
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._file = path.open("w", encoding="utf-8")

    def line(self, message: str) -> None:
        ts = datetime.now(timezone.utc).isoformat()
        row = f"{ts} {message}"
        print(row)
        self._file.write(row + "\n")
        self._file.flush()

    def block(self, title: str, payload: Any) -> None:
        self.line(f"=== {title} ===")
        if isinstance(payload, (dict, list)):
            text = json.dumps(payload, indent=2, ensure_ascii=False)
        else:
            text = str(payload)
        for line in text.splitlines():
            self.line(line)

    def close(self) -> None:
        self._file.close()


def env(name: str, default: str | None = None, required: bool = False) -> str:
    value = os.environ.get(name, default)
    if required and (value is None or value.strip() == ""):
        print(f"Missing required environment variable: {name}", file=sys.stderr)
        sys.exit(2)
    return value or ""


def base_headers(auth_token: str, user_uuid: str, correlation_id: str) -> dict[str, str]:
    return {
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
        "auth-token": auth_token,
        "x-user-uuid": user_uuid,
        "x-correlation-id": correlation_id,
    }


def post_mcp(
    log: Logger,
    url: str,
    headers: dict[str, str],
    body: dict[str, Any] | None,
    step: str,
    session_id: str | None,
) -> tuple[int, dict[str, str], str, str | None]:
    merged = dict(headers)
    if session_id:
        merged["Mcp-Session-Id"] = session_id

    payload = json.dumps(body).encode("utf-8") if body is not None else b""
    log.block(f"REQUEST {step}", {"url": url, "headers": redact_headers(merged), "body": body})

    request = urllib.request.Request(url, data=payload, method="POST")
    for key, value in merged.items():
        request.add_header(key, value)

    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            status = response.status
            resp_headers = {k: v for k, v in response.headers.items()}
            raw = response.read().decode("utf-8", errors="replace")
            new_session = resp_headers.get("Mcp-Session-Id") or resp_headers.get("mcp-session-id")
            log.block(
                f"RESPONSE {step}",
                {"status": status, "headers": resp_headers, "body": raw},
            )
            return status, resp_headers, raw, new_session
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        resp_headers = {k: v for k, v in error.headers.items()}
        log.block(
            f"RESPONSE {step} (HTTP error)",
            {"status": error.code, "headers": resp_headers, "body": raw},
        )
        return error.code, resp_headers, raw, None


def redact_headers(headers: dict[str, str]) -> dict[str, str]:
    out = dict(headers)
    if "auth-token" in out:
        token = out["auth-token"]
        out["auth-token"] = token[:8] + "…" if len(token) > 8 else "[redacted]"
    return out


def parse_json_body(raw: str) -> Any:
    raw = raw.strip()
    if not raw:
        return None
    # Streamable HTTP may return SSE: lines starting with "data:"
    if raw.startswith("data:") or "\ndata:" in raw:
        chunks: list[Any] = []
        for line in raw.splitlines():
            line = line.strip()
            if line.startswith("data:"):
                piece = line[5:].strip()
                if piece and piece != "[DONE]":
                    try:
                        chunks.append(json.loads(piece))
                    except json.JSONDecodeError:
                        chunks.append(piece)
        return chunks if len(chunks) != 1 else chunks[0]
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return raw


def tool_call(
    log: Logger,
    url: str,
    headers: dict[str, str],
    session_id: str,
    step: str,
    name: str,
    arguments: dict[str, Any],
) -> tuple[int, str, Any]:
    body = {
        "jsonrpc": "2.0",
        "id": next_id(),
        "method": "tools/call",
        "params": {"name": name, "arguments": arguments},
    }
    status, _, raw, _ = post_mcp(log, url, headers, body, step, session_id)
    parsed = parse_json_body(raw)
    return status, raw, parsed


def unwrap_tool_result(parsed: Any) -> Any:
    """Extract tool payload from MCP tools/call JSON (-RPC or SSE)."""
    if isinstance(parsed, list):
        parsed = parsed[-1]
    if not isinstance(parsed, dict):
        return parsed
    result = parsed.get("result")
    if not isinstance(result, dict):
        return parsed
    content = result.get("content")
    if isinstance(content, list) and content:
        first = content[0]
        if isinstance(first, dict) and first.get("type") == "text":
            text = first.get("text", "")
            try:
                return json.loads(text)
            except (json.JSONDecodeError, TypeError):
                return text
    return result


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    log_file = env("MCP_LOG_FILE", "")
    log_path = Path(log_file) if log_file else repo_root / "logs" / "mcp-smoke-test.log"
    if not log_path.is_absolute():
        log_path = repo_root / log_path
    log = Logger(log_path)

    auth_token = env("MCP_AUTH_TOKEN", required=True)
    user_uuid = env("MCP_USER_UUID", required=True)
    url = env("MCP_URL", "http://localhost:8088/mcp")
    correlation_id = env("MCP_CORRELATION_ID", f"mcp-smoke-{stamp}")
    tenant = env("MCP_TENANT", "pg")
    mobile = env("MCP_MOBILE", "")
    property_id = env("MCP_PROPERTY_ID", "")
    property_tenant = env("MCP_PROPERTY_TENANT", tenant if "." in tenant else "pg.citya")
    pgr_service_code = env("MCP_PGR_SERVICE_CODE", "Streetlight")
    run_prepare = env("MCP_RUN_PREPARE", "false").lower() in ("1", "true", "yes")
    run_confirm = env("MCP_RUN_CONFIRM", "false").lower() in ("1", "true", "yes")

    headers = base_headers(auth_token, user_uuid, correlation_id)
    failures = 0

    log.line(f"Log file: {log_path}")
    log.line(f"MCP_URL={url} correlation_id={correlation_id}")

    # --- initialize ---
    init_body = {
        "jsonrpc": "2.0",
        "id": next_id(),
        "method": "initialize",
        "params": {
            "protocolVersion": "2025-03-26",
            "capabilities": {},
            "clientInfo": {"name": "mcp-smoke-test", "version": "1.0.0"},
        },
    }
    status, resp_headers, raw, session_from_init = post_mcp(
        log, url, headers, init_body, "initialize", None
    )
    session_id = session_from_init or resp_headers.get("Mcp-Session-Id") or resp_headers.get(
        "mcp-session-id"
    )
    if not session_id:
        log.line("FAIL: No Mcp-Session-Id in initialize response headers. Use curl -i or check Accept header.")
        log.close()
        return 1
    if status >= 400:
        failures += 1
    log.line(f"Mcp-Session-Id={session_id}")

    # --- notifications/initialized ---
    status, _, _, _ = post_mcp(
        log,
        url,
        headers,
        {"jsonrpc": "2.0", "method": "notifications/initialized"},
        "notifications/initialized",
        session_id,
    )
    if status >= 400:
        failures += 1

    # --- list_services ---
    status, _, parsed = tool_call(
        log, url, headers, session_id, "tools/list_services", "list_services", {}
    )
    services_payload = unwrap_tool_result(parsed)
    log.block("PARSED list_services", services_payload)
    if status >= 400:
        failures += 1

    service_ids: list[str] = []
    if isinstance(services_payload, dict):
        services = services_payload.get("services")
        if isinstance(services, list):
            for item in services:
                if isinstance(item, dict) and item.get("id"):
                    service_ids.append(str(item["id"]))
    if not service_ids:
        service_ids = ["pgr", "property", "advertisement", "venue-booking"]

    # --- describe_operation for search on each service ---
    for service in service_ids:
        status, _, parsed = tool_call(
            log,
            url,
            headers,
            session_id,
            f"tools/describe_operation/{service}/search",
            "describe_operation",
            {"service": service, "operation": "search"},
        )
        if status >= 400:
            failures += 1

    # --- lookup_master (pgr) ---
    status, _, _ = tool_call(
        log,
        url,
        headers,
        session_id,
        "tools/lookup_master",
        "lookup_master",
        {"service": "pgr", "master": "ServiceDefs", "tenantId": tenant},
    )
    if status >= 400:
        failures += 1

    # --- search pgr ---
    pgr_filters: dict[str, Any] = {"tenantId": tenant}
    if mobile:
        pgr_filters["mobileNumber"] = mobile
    status, _, parsed = tool_call(
        log,
        url,
        headers,
        session_id,
        "tools/search/pgr",
        "search",
        {"service": "pgr", "filters": pgr_filters, "page": 0, "size": 10},
    )
    pgr_result = unwrap_tool_result(parsed)
    if status >= 400:
        failures += 1

    service_request_id = None
    if isinstance(pgr_result, dict):
        items = pgr_result.get("items") or pgr_result.get("results")
        if isinstance(items, list) and items:
            first = items[0]
            if isinstance(first, dict):
                service_request_id = first.get("serviceRequestId") or (
                    first.get("service", {}) or {}
                ).get("serviceRequestId")

    # --- get_status (if we found an id) ---
    if service_request_id:
        status, _, _ = tool_call(
            log,
            url,
            headers,
            session_id,
            "tools/get_status/pgr",
            "get_status",
            {"service": "pgr", "id": service_request_id, "tenantId": tenant},
        )
        if status >= 400:
            failures += 1
    else:
        log.line("SKIP get_status (no serviceRequestId from PGR search)")

    # --- property search + billing (optional) ---
    if property_id:
        status, _, _ = tool_call(
            log,
            url,
            headers,
            session_id,
            "tools/search/property",
            "search",
            {
                "service": "property",
                "filters": {"tenantId": property_tenant, "propertyIds": [property_id]},
                "page": 0,
                "size": 20,
            },
        )
        if status >= 400:
            failures += 1

        status, _, _ = tool_call(
            log,
            url,
            headers,
            session_id,
            "tools/get_pending_bill",
            "get_pending_bill",
            {
                "businessService": "PT",
                "consumerCode": property_id,
                "tenantId": property_tenant,
            },
        )
        if status >= 400:
            failures += 1

        status, _, _ = tool_call(
            log,
            url,
            headers,
            session_id,
            "tools/get_payment_link",
            "get_payment_link",
            {
                "businessService": "PT",
                "consumerCode": property_id,
                "tenantId": property_tenant,
            },
        )
        if status >= 400:
            failures += 1
    else:
        log.line("SKIP property search / billing (set MCP_PROPERTY_ID to enable)")

    # --- prepare / confirm (optional writes) ---
    confirmation_token = None
    if run_prepare:
        status, _, parsed = tool_call(
            log,
            url,
            headers,
            session_id,
            "tools/prepare_action/pgr/create",
            "prepare_action",
            {
                "service": "pgr",
                "operation": "create",
                "payload": {
                    "tenantId": tenant,
                    "serviceCode": pgr_service_code,
                    "description": "MCP smoke test — safe to close",
                    "address": "Automated smoke test",
                },
            },
        )
        prepare_result = unwrap_tool_result(parsed)
        if isinstance(prepare_result, dict):
            confirmation_token = prepare_result.get("confirmationToken")
        if status >= 400:
            failures += 1

        if run_confirm and confirmation_token:
            status, _, _ = tool_call(
                log,
                url,
                headers,
                session_id,
                "tools/confirm_action",
                "confirm_action",
                {"confirmationToken": confirmation_token},
            )
            if status >= 400:
                failures += 1
        elif run_confirm:
            log.line("SKIP confirm_action (no confirmationToken from prepare)")
    else:
        log.line("SKIP prepare_action / confirm_action (set MCP_RUN_PREPARE=true to enable)")

    log.line(f"Done. failures={failures} log={log_path}")
    log.close()
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
