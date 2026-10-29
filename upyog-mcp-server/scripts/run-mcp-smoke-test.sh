#!/usr/bin/env bash
# Loads scripts/mcp-smoke-test.env then runs mcp-smoke-test.py
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="$ROOT/scripts/mcp-smoke-test.env"
EXAMPLE_FILE="$ROOT/scripts/mcp-smoke-test.env.example"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  set -a
  source "$ENV_FILE"
  set +a
  echo "Loaded config: $ENV_FILE"
elif [[ -f "$EXAMPLE_FILE" ]]; then
  echo "No $ENV_FILE — copy the example and edit your token:" >&2
  echo "  cd upyog-mcp-server" >&2
  echo "  cp scripts/mcp-smoke-test.env.example scripts/mcp-smoke-test.env" >&2
  exit 2
fi

if [[ -z "${MCP_AUTH_TOKEN:-}" || -z "${MCP_USER_UUID:-}" ]]; then
  echo "Required in $ENV_FILE (or environment): MCP_AUTH_TOKEN, MCP_USER_UUID" >&2
  exit 2
fi

exec python3 "$ROOT/scripts/mcp-smoke-test.py" "$@"
