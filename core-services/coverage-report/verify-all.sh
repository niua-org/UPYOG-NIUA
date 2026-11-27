#!/usr/bin/env bash
set -uo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WS="$(cd "$ROOT/.." && pwd)"
LOG="$ROOT/coverage-report/verify-all.log"
: > "$LOG"

MAVEN_MODULES=(
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

BUSINESS_MODULES=(
  finance-collections-voucher-consumer
  dashboard-analytics
)

echo "verify-all $(date -u +%Y-%m-%dT%H:%M:%SZ)" | tee -a "$LOG"
ok=0
fail=0
for mod in "${MAVEN_MODULES[@]}"; do
  echo "=== $mod ===" | tee -a "$LOG"
  if (cd "$ROOT/$mod" && mvn -q clean verify >> "$LOG" 2>&1); then
    echo "OK $mod" | tee -a "$LOG"
    ok=$((ok + 1))
  else
    echo "FAIL $mod" | tee -a "$LOG"
    fail=$((fail + 1))
  fi
done

for mod in "${BUSINESS_MODULES[@]}"; do
  echo "=== $mod ===" | tee -a "$LOG"
  if (cd "$WS/business-services/$mod" && mvn -q clean verify >> "$LOG" 2>&1); then
    echo "OK $mod" | tee -a "$LOG"
    ok=$((ok + 1))
  else
    echo "FAIL $mod" | tee -a "$LOG"
    fail=$((fail + 1))
  fi
done

echo "=== pdf-service (npm test) ===" | tee -a "$LOG"
if (cd "$ROOT/pdf-service" && npm ci --silent && npm test --silent >> "$LOG" 2>&1); then
  echo "OK pdf-service" | tee -a "$LOG"
  ok=$((ok + 1))
else
  echo "FAIL pdf-service" | tee -a "$LOG"
  fail=$((fail + 1))
fi

echo "SUMMARY ok=$ok fail=$fail" | tee -a "$LOG"
exit $(( fail > 0 ? 1 : 0 ))
