# UPYOG MCP — manual test plan (Postman / curl)

Hand this document to integration, QA, or chatbot teams validating **upyog-mcp-server** against NIUA UAT or a local instance.

For assistant behaviour and evaluation prompts, see [chatbot-team-guide.md](./chatbot-team-guide.md). For architecture and operator config, see [README.md](../README.md).

---

## 1. Scope

| In scope | Out of scope |
|----------|----------------|
| Streamable HTTP MCP on `/mcp` (JSON-RPC 2.0) | Chatbot model / RAG / memory |
| Session validation and nine MCP tools | Direct calls to business services bypassing MCP |
| Citizen token on NIUA UAT (`https://niuatt.niua.in`) | Load / security penetration testing |

---

## 2. Environments

### 2.1 Local MCP + UAT gateway (recommended for first tests)

| Item | Value |
|------|--------|
| MCP URL | `POST http://localhost:8088/mcp` |
| Downstream gateway | `UPYOG_GATEWAY_BASE_URL=https://niuatt.niua.in` (default in `application.yml`) |
| UI origin headers | `UPYOG_UI_BASE_URL=https://niuatt.niua.in` (sent as `Origin` / `Referer` on gateway calls) |
| Trust gateway on MCP hop | `UPYOG_MCP_TRUST_GATEWAY_IDENTITY=false` (default for local Postman) |
| Secret | `UPYOG_MCP_TOKEN_SECRET` — at least 32 characters (required at startup) |

Start server:

```bash
cd upyog-mcp-server
export UPYOG_MCP_TOKEN_SECRET='local-dev-only-change-me-32chars'
mvn spring-boot:run
```

Automated regression: `mvn test`.

**Automated MCP smoke (handshake + all tools, request/response log file):** see **[scripts/README.md](../scripts/README.md)**.

```bash
cd upyog-mcp-server
cp scripts/mcp-smoke-test.env.example scripts/mcp-smoke-test.env
# edit scripts/mcp-smoke-test.env — MCP_AUTH_TOKEN, MCP_USER_UUID, MCP_MOBILE, …

# terminal 1
export UPYOG_MCP_TOKEN_SECRET='local-dev-only-change-me-32chars'
mvn spring-boot:run

# terminal 2
./scripts/run-mcp-smoke-test.sh
```

Logs: `logs/mcp-smoke-test.log` (overwritten each run).

### 2.2 Production-like (chatbot → gateway → MCP)

| Item | Value |
|------|--------|
| MCP URL | `POST https://<gateway>/upyog-mcp-server/mcp` |
| Session header | `Auth-Token: <citizen access token>` |
| Gateway forwards | `x-user-info`, `x-pass-through-gateway: true`, `Auth-Token` |
| MCP config | `UPYOG_MCP_TRUST_GATEWAY_IDENTITY=true` |

MDMS must allow the caller’s role on **`/upyog-mcp-server/mcp`** (hop 1). Business APIs still use gateway RBAC on hop 2.

---

## 3. Obtain test credentials (NIUA UAT)

1. Open [NIUA UAT citizen UI](https://niuatt.niua.in/upyog-ui/citizen/) and sign in.
2. DevTools → **Network** → any API call after login.
3. Copy:
   - **Access token** → use as `auth-token` (local) or `Auth-Token` (via gateway).
   - **Citizen uuid** → `RequestInfo.userInfo.uuid` → use as `x-user-uuid`.
4. Optional: copy full **`RequestInfo.userInfo`** JSON from a `POST /user/_search` request body → `x-user-info` header (or Base64 in `x-user-info-base64` if the JSON is large).

Session validation on direct `:8088` uses the same contract as the citizen UI: **`POST /user/_search`** with `authToken` inside `RequestInfo` (not a separate auth story from the UI).

---

## 4. Common HTTP headers (direct local `:8088`)

Use on **every** request to `http://localhost:8088/mcp`:

```http
Content-Type: application/json
Accept: application/json, text/event-stream
auth-token: <citizen access token>
x-user-uuid: <citizen uuid>
x-correlation-id: <your trace id, e.g. local-test-1>
```

Optional:

```http
x-user-info: <full userInfo JSON from UI>
x-user-info-base64: <standard Base64 of UTF-8 userInfo JSON>
```

**Required after `initialize` (Streamable HTTP):** every request that is not the first `initialize` must include:

```http
Mcp-Session-Id: <value from the initialize response header>
```

If you call `tools/call` without this header, the server returns **`Session ID missing`** (from the MCP transport layer). Auth can succeed (`mcp_auth_succeeded` in logs) and you will still get this error.

Do **not** put `authToken`, `uuid`, or `RequestInfo` inside MCP JSON-RPC tool `arguments`.

### 4.1 Order of calls (do not skip)

```text
1. POST initialize          → copy response header Mcp-Session-Id
2. POST notifications/initialized   (same auth headers + Mcp-Session-Id)
3. POST tools/call ...      (same auth headers + Mcp-Session-Id)
```

Use the **same** `Mcp-Session-Id` for steps 2–3 in one Postman run. After server restart, run `initialize` again.

#### Why `notifications/initialized`?

This is part of the [MCP lifecycle](https://modelcontextprotocol.io/), not UPYOG business logic. Flow:

1. **Client → server:** `initialize` (“what protocol and capabilities?”).
2. **Server → client:** result (protocol version, tools, instructions).
3. **Client → server:** `notifications/initialized` (“I finished setup; we can use tools now.”).

The message has **no `id`** because it is a JSON-RPC **notification** (no response body expected). Real MCP clients (Cursor, Claude Desktop) send it automatically after `initialize`. Skipping it can leave the Streamable HTTP session in a “not ready” state on some servers, so **`tools/call` may fail** even when auth and `Mcp-Session-Id` are correct.

UPYOG auth (`auth-token`, `/user/_search`) is separate and runs in servlet filters **before** this MCP handshake.

---

## 5. Test cases

Record **Pass / Fail**, response snippet, and **`correlationId`** for each case.

### TC-01 — Health (optional)

| Step | Action |
|------|--------|
| Request | `GET http://localhost:8088/actuator/health` |
| Expected | HTTP 200, `"status":"UP"` |

### TC-02 — MCP initialize

| Step | Action |
|------|--------|
| Method | `initialize` |
| Body | See [JSON TC-02](#json-tc-02-initialize) |
| Expected HTTP | 200 |
| Expected logs | `mcp_auth_succeeded correlationId=... uuid=...` |
| Expected body | JSON-RPC result with server capabilities |
| **Required** | Response header **`Mcp-Session-Id`** — copy it for all following requests |

### TC-03 — MCP initialized notification

| Step | Action |
|------|--------|
| Headers | Same as §4 **plus** `Mcp-Session-Id` from TC-02 |
| Method | `notifications/initialized` (no `id` field) |
| Expected | HTTP 202/200 per transport; no auth error |

### TC-04 — Session failure (negative)

| Step | Action |
|------|--------|
| Request | Same as TC-02 but **omit** `auth-token` |
| Expected HTTP | 401 |
| Expected JSON | `"code":"AUTHENTICATION_REQUIRED"` |

### TC-05 — Session failure — missing uuid (negative)

| Step | Action |
|------|--------|
| Request | TC-02 with `auth-token` but **no** `x-user-uuid` / `x-user-info` |
| Expected HTTP | 401 |
| Expected JSON | `"code":"AUTHENTICATION_FAILED"` with `reason` mentioning uuid |

### TC-06 — `list_services`

| Step | Action |
|------|--------|
| Headers | Same as §4 **plus** `Mcp-Session-Id` from TC-02 (TC-03 recommended first) |
| Tool | `list_services`, arguments `{}` |
| Expected | Tool result lists services (e.g. `pgr`, `property`, …) and operations; `correlationId` matches header |
| Fail signals | `AUTHENTICATION_*`, empty error map |

### TC-07 — `describe_operation` (PGR search)

| Step | Action |
|------|--------|
| Tool | `describe_operation` with `service=pgr`, `operation=search` |
| Expected | Input schema with allowed filters (`tenantId`, optional `serviceRequestId`, `mobileNumber`, …); no internal URLs |

### TC-08 — `search` PGR (read, gateway hop 2)

| Step | Action |
|------|--------|
| Tool | `search` with `service=pgr` and filters for a tenant the citizen can access |
| Example filters | `tenantId: pg`, `mobileNumber: <logged-in mobile>` |
| Expected logs | `downstream_call path=/pgr-services/v2/request/_search read=true status=200` |
| Expected tool result | Masked list or empty list; not `NOT_AUTHORIZED` if UAT RBAC allows citizen search |

Adjust `tenantId` / ids using TC-06 and UAT data.

### TC-09 — `lookup_master` (MDMS read)

| Step | Action |
|------|--------|
| Tool | `lookup_master`, `service=pgr`, `master=ServiceDefs`, `tenantId=pg` |
| Expected | Master data or controlled `NOT_FOUND` / `NOT_AUTHORIZED`; not auth failure at MCP layer |

### TC-10 — `search` property (optional)

| Step | Action |
|------|--------|
| Tool | `search`, `service=property`, filters with real `tenantId` and `propertyIds` for the token |
| Expected | HTTP 200 downstream on `/property-services/...`; masked property fields |

### TC-11 — `get_pending_bill` (optional)

| Step | Action |
|------|--------|
| Tool | `get_pending_bill` with `businessService=PT`, `consumerCode=<property id>`, `tenantId=<ulb>` |
| Expected | Bill summary or explicit no-bill; no payment execution |

### TC-12 — `get_payment_link` (optional)

| Step | Action |
|------|--------|
| Tool | `get_payment_link` with same codes as TC-11 |
| Expected | Citizen **page URL** only (host from `UPYOG_UI_BASE_URL`); no collection create |

### TC-13 — `prepare_action` PGR create (write — no commit)

| Step | Action |
|------|--------|
| Tool | `prepare_action`, `service=pgr`, `operation=create`, payload per `describe_operation` for `create` |
| Expected | Summary + **`confirmationToken`**; **no** new grievance in UI yet |
| Expected logs | No downstream write to PGR create endpoint |

Use realistic `serviceCode` from TC-09. Treat description/address as test data only.

### TC-14 — `confirm_action` (write — **creates real data**)

| Step | Action |
|------|--------|
| Preconditions | `REDIS_ENABLED=true` and `REDIS_URI` set; else expect `CONFIRMATION_STORE_UNAVAILABLE` |
| Tool | `confirm_action` with `confirmationToken` from TC-13 |
| Expected | Success once; repeating same token → `TOKEN_ALREADY_USED` |
| Warning | **Only run in UAT with approval** — creates a real grievance |

### TC-15 — Payload guard (negative)

| Step | Action |
|------|--------|
| Tool | `search` with extra forbidden fields in `filters` (e.g. `authToken`, `RequestInfo`) |
| Expected | `INVALID_INPUT` or schema rejection; no gateway call with attacker-controlled token |

### TC-16 — Correlation id propagation

| Step | Action |
|------|--------|
| Request | TC-06 with `x-correlation-id: qa-run-2026-10-16-01` |
| Expected | Same value in tool result `correlationId`, response header `x-correlation-id`, and MCP logs `CORRELATION_ID` |

---

## 6. JSON payloads (copy-paste)

Replace placeholders: `<TOKEN>`, `<UUID>`, `<TENANT>`, `<PROPERTY_ID>`, `<MOBILE>`.

### JSON TC-02 — initialize

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {},
    "clientInfo": { "name": "postman", "version": "1.0.0" }
  }
}
```

### JSON TC-03 — initialized notification

```json
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}
```

### JSON TC-06 — list_services

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "list_services",
    "arguments": {}
  }
}
```

### JSON TC-07 — describe_operation

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "describe_operation",
    "arguments": {
      "service": "pgr",
      "operation": "search"
    }
  }
}
```

### JSON TC-08 — search PGR

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "search",
    "arguments": {
      "service": "pgr",
      "filters": {
        "tenantId": "<TENANT>",
        "mobileNumber": "<MOBILE>"
      },
      "page": 0,
      "size": 10
    }
  }
}
```

### JSON TC-09 — lookup_master

```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "tools/call",
  "params": {
    "name": "lookup_master",
    "arguments": {
      "service": "pgr",
      "master": "ServiceDefs",
      "tenantId": "<TENANT>"
    }
  }
}
```

### JSON TC-10 — search property

```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "tools/call",
  "params": {
    "name": "search",
    "arguments": {
      "service": "property",
      "filters": {
        "tenantId": "<TENANT>",
        "propertyIds": ["<PROPERTY_ID>"]
      },
      "page": 0,
      "size": 20
    }
  }
}
```

### JSON TC-11 — get_pending_bill

```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "tools/call",
  "params": {
    "name": "get_pending_bill",
    "arguments": {
      "businessService": "PT",
      "consumerCode": "<PROPERTY_ID>",
      "tenantId": "<TENANT>"
    }
  }
}
```

### JSON TC-12 — get_payment_link

```json
{
  "jsonrpc": "2.0",
  "id": 8,
  "method": "tools/call",
  "params": {
    "name": "get_payment_link",
    "arguments": {
      "businessService": "PT",
      "consumerCode": "<PROPERTY_ID>",
      "tenantId": "<TENANT>"
    }
  }
}
```

### JSON TC-13 — prepare_action (example skeleton)

Use `describe_operation` for `pgr` / `create` for required fields before running.

```json
{
  "jsonrpc": "2.0",
  "id": 9,
  "method": "tools/call",
  "params": {
    "name": "prepare_action",
    "arguments": {
      "service": "pgr",
      "operation": "create",
      "payload": {
        "tenantId": "<TENANT>",
        "serviceCode": "<FROM_ServiceDefs>",
        "description": "MCP manual test complaint — safe to close",
        "address": "Test address"
      }
    }
  }
}
```

### JSON TC-14 — confirm_action

```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "confirm_action",
    "arguments": {
      "confirmationToken": "<FROM_TC-13>"
    }
  }
}
```

---

## 7. Example curl (full handshake + list_services)

**Step A — initialize** (capture session id from headers):

```bash
curl -i --location 'http://localhost:8088/mcp' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json, text/event-stream' \
  --header 'auth-token: <TOKEN>' \
  --header 'x-user-uuid: <UUID>' \
  --header 'x-correlation-id: local-test-1' \
  --data '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-03-26",
      "capabilities": {},
      "clientInfo": { "name": "curl", "version": "1.0.0" }
    }
  }'
```

Look for a response line: `Mcp-Session-Id: <SESSION>`.

**Step B — initialized notification:**

```bash
curl --location 'http://localhost:8088/mcp' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json, text/event-stream' \
  --header 'auth-token: <TOKEN>' \
  --header 'x-user-uuid: <UUID>' \
  --header 'x-correlation-id: local-test-1' \
  --header 'Mcp-Session-Id: <SESSION>' \
  --data '{"jsonrpc":"2.0","method":"notifications/initialized"}'
```

**Step C — list_services:**

```bash
curl --location 'http://localhost:8088/mcp' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json, text/event-stream' \
  --header 'auth-token: <TOKEN>' \
  --header 'x-user-uuid: <UUID>' \
  --header 'x-correlation-id: local-test-1' \
  --header 'Mcp-Session-Id: <SESSION>' \
  --data '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "list_services",
      "arguments": {}
    }
  }'
```

---

## 8. Log lines to verify (direct local)

| Log message | Meaning |
|-------------|---------|
| `mcp_auth_succeeded correlationId=... uuid=...` | Session validated via `/user/_search` |
| `gateway_user_search ... curl=...` | Replay curl for session check against UAT |
| `downstream_call path=... status=200` | Business read/write reached gateway successfully |
| `mcp_session_check_failed code=DOWNSTREAM_UNAVAILABLE` | UAT/gateway 5xx during session check — not invalid token |
| `AUDIT` logger line | Tool name, outcome code, redacted args |

---

## 9. HTTP vs tool error codes

| Layer | Code | HTTP | Meaning |
|-------|------|------|---------|
| MCP filter | `AUTHENTICATION_REQUIRED` | 401 | No token |
| MCP filter | `AUTHENTICATION_FAILED` | 401 | Bad session / uuid / user search |
| MCP filter | `DOWNSTREAM_UNAVAILABLE` | 503 | UAT error during `/user/_search` |
| Tool result | `NOT_AUTHORIZED` | 200 body | Gateway RBAC denied business API |
| Tool result | `INVALID_INPUT` | 200 body | Bad or forbidden tool arguments |
| Tool result | `CONFIRMATION_STORE_UNAVAILABLE` | 200 body | Redis off — confirm not supported |
| Transport | `Session ID missing` | error body | Run `initialize` first; send header `Mcp-Session-Id` on all later POSTs |

Tool failures still return HTTP 200 with an error object inside the MCP tool result (see [README — Errors](../README.md#errors)).

---

## 10. Sign-off checklist

- [ ] TC-01–TC-03 pass (MCP handshake + auth)
- [ ] TC-04–TC-05 pass (negative auth)
- [ ] TC-06–TC-09 pass (catalog + PGR read path)
- [ ] TC-10–TC-12 pass or N/A with documented reason
- [ ] TC-13 pass (prepare without create)
- [ ] TC-14 only if Redis enabled and UAT write approved
- [ ] TC-15–TC-16 pass
- [ ] `mvn test` green on release commit

**Tester name / date / environment / token role (CITIZEN):** _______________
