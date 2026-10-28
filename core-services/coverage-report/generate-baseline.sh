#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPORT_DIR="$ROOT/coverage-report"
mkdir -p "$REPORT_DIR/raw"

MODULES=(
  core-smoke-tests
  egov-idgen
  egov-indexer
  egov-notification-sms
  egov-pg-service
  egov-user
  egov-workflow-v2
  gateway
  gis-service
  mdms-v2
  tenant
  upyog-draft-service
)

echo "# Coverage baseline (JDK 17)" > "$REPORT_DIR/baseline.md"
echo "Generated: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$REPORT_DIR/baseline.md"
echo "" >> "$REPORT_DIR/baseline.md"

for mod in "${MODULES[@]}"; do
  echo "=== $mod ==="
  cd "$ROOT/$mod"
  status="OK"
  if ! mvn -q clean test -DskipTests=false 2>"$REPORT_DIR/raw/${mod}.log"; then
    status="BUILD_FAILED"
  fi
  xml="$ROOT/$mod/target/site/jacoco/jacoco.xml"
  if [[ -f "$xml" ]]; then
    cp "$xml" "$REPORT_DIR/raw/${mod}-jacoco.xml"
    python3 "$REPORT_DIR/parse_jacoco.py" "$xml" "$mod" >> "$REPORT_DIR/baseline.md"
  else
    echo "## $mod" >> "$REPORT_DIR/baseline.md"
    echo "Status: $status (no jacoco.xml — see raw/${mod}.log)" >> "$REPORT_DIR/baseline.md"
    echo "" >> "$REPORT_DIR/baseline.md"
  fi
done
