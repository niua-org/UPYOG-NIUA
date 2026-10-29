#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="/tmp/apache-maven-3.9.6/bin:${JAVA_HOME}/bin:${PATH}"
ROOT="/workspace/municipal-services"
VERIFY="$ROOT/testing/verify_module.sh"
RECORD="$ROOT/testing/record_module_coverage.py"

extra_for() {
  case "$1" in
    birth-death-services|pqm|pqm-anomaly-finder|rainmaker-pgr) echo "-Dspring-boot.repackage.skip=true" ;;
    *) echo "" ;;
  esac
}

MODULES=(
  advertisement-service asset-calculator asset-services birth-death-services
  challan-generation challan-generation-calculator cnd-service cnd-calculator
  echallan-services echallan-calculator egov-user-event estate-management
  ewaste-services land-services pgr-ai-services pqm pqm-anomaly-finder
  pt-services-v2 tp-services vehicle vendor-management
  ws-calculator sw-calculator pt-calculator-v2 community-hall-booking property-services
)

LOG="/tmp/deep-pass-verify.log"
: > "$LOG"
for m in "${MODULES[@]}"; do
  ex=$(extra_for "$m")
  echo "=== $m ===" | tee -a "$LOG"
  if bash "$VERIFY" "$m" $ex >> "$LOG" 2>&1; then
    python3 "$RECORD" "$m" | tee -a "$LOG"
    echo "PASS $m" | tee -a "$LOG"
  else
    echo "FAIL $m" | tee -a "$LOG"
  fi
done
