#!/usr/bin/env bash
# Records mvn test outcomes per module (best-effort).
set -uo pipefail
export PATH="/tmp/apache-maven-3.9.6/bin:${PATH}"
SETTINGS="-s /workspace/municipal-services/.mvn/settings-public.xml"
OUT=/workspace/municipal-services/testing/module-test-run-results.txt
: > "$OUT"
for pom in /workspace/municipal-services/*/pom.xml; do
  module=$(basename "$(dirname "$pom")")
  [[ "$module" == "core-smoke-tests" ]] && continue
  echo "Running $module..." >&2
  if mvn -f "$pom" $SETTINGS -q test >>"$OUT" 2>&1; then
    echo "$module PASS" >> "$OUT.summary"
  else
    echo "$module FAIL" >> "$OUT.summary"
  fi
done
