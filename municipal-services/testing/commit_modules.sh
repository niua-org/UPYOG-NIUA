#!/usr/bin/env bash
set -euo pipefail
cd /workspace
META=/workspace/municipal-services/testing/modules-test-metadata.json
modules=$(python3 -c "import json; print(' '.join(m['module'] for m in json.load(open('$META'))))")
for m in $modules; do
  if git status --porcelain "municipal-services/$m" | grep -q .; then
    smoke_pkg=$(python3 -c "print('$m'.replace('-',''))")
    git add "municipal-services/$m" || true
    git add "municipal-services/core-smoke-tests/src/test/java/org/niua/smoke/${smoke_pkg}" 2>/dev/null || true
    git add "municipal-services/core-smoke-tests/src/test/java/org/niua/smoke/$(python3 - <<PY
import re
m='$m'
parts=m.split('-')
print(''.join(p.capitalize() for p in parts))
PY
)" 2>/dev/null || true
    if git diff --cached --quiet; then
      continue
    fi
    git commit -m "tests: add layered tests and smoke registration for ${m}"
  fi
done
# shared infra
for path in municipal-services/testing municipal-services/.mvn municipal-services/core-smoke-tests/src/test/resources/services.yaml municipal-services/docs/TEST_STRATEGY_REPORT.md; do
  git add "$path" 2>/dev/null || true
done
if ! git diff --cached --quiet; then
  git commit -m "tests: shared municipal-services test tooling and strategy report update"
fi
