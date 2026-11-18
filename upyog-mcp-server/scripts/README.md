# MCP smoke test scripts

Automated handshake (`initialize` → `Mcp-Session-Id` → `notifications/initialized`) and all MCP tools, with full request/response logging.

## One-time setup

```bash
cd upyog-mcp-server

cp scripts/mcp-smoke-test.env.example scripts/mcp-smoke-test.env
```

Edit **`scripts/mcp-smoke-test.env`** (gitignored) with your UAT citizen token and uuid:

```bash
export MCP_AUTH_TOKEN='...'
export MCP_USER_UUID='f789d679-352d-4aec-8198-4d61af1e0566'
export MCP_MOBILE='8010012414'
```

## Every test run

**Terminal 1 — start MCP** (if not already running):

```bash
cd upyog-mcp-server
export UPYOG_MCP_TOKEN_SECRET='local-dev-only-change-me-32chars'
mvn spring-boot:run
```

**Terminal 2 — run smoke test**:

```bash
cd upyog-mcp-server
chmod +x scripts/run-mcp-smoke-test.sh   # once
./scripts/run-mcp-smoke-test.sh
```

The shell wrapper loads **`scripts/mcp-smoke-test.env`** automatically. Environment variables you export in the shell **override** the file.

**Log file:** `logs/mcp-smoke-test.log` — **overwritten on every run** (path printed at start). Override with `MCP_LOG_FILE` in env.

**Exit code:** `0` success, `1` at least one HTTP step failed, `2` missing config.

## Config reference (`mcp-smoke-test.env`)

| Variable | Required | Description |
|----------|----------|-------------|
| `MCP_URL` | no | Default `http://localhost:8088/mcp` |
| `MCP_AUTH_TOKEN` | **yes** | Citizen access token from UAT UI |
| `MCP_USER_UUID` | **yes** | `RequestInfo.userInfo.uuid` |
| `MCP_CORRELATION_ID` | no | Trace id in logs and tool results |
| `MCP_TENANT` | no | Default `pg` |
| `MCP_MOBILE` | no | Logged-in mobile for PGR `search` |
| `MCP_PROPERTY_ID` | no | If set, runs property search + billing tools |
| `MCP_PROPERTY_TENANT` | no | e.g. `pg.citya` |
| `MCP_PGR_SERVICE_CODE` | no | For optional `prepare_action` |
| `MCP_RUN_PREPARE` | no | `true` to run prepare (no real write until confirm) |
| `MCP_RUN_CONFIRM` | no | `true` creates real PGR; needs Redis on MCP |
| `MCP_LOG_FILE` | no | Default `logs/mcp-smoke-test.log` (overwritten each run) |

## Without the config file

```bash
cd upyog-mcp-server
export MCP_AUTH_TOKEN='...'
export MCP_USER_UUID='...'
export MCP_MOBILE='8010012414'
python3 scripts/mcp-smoke-test.py
```

## Manual test cases

See [docs/mcp-manual-test-plan.md](../docs/mcp-manual-test-plan.md).
