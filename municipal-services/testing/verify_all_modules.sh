#!/usr/bin/env bash
# Full municipal-services verify sweep (JDK 17).
set -uo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:${PATH}"
SETTINGS="-s /workspace/municipal-services/.mvn/settings-public.xml"
ROOT="/workspace/municipal-services"
LOG="${1:-/tmp/verify-all-final.log}"

BOOT22_SKIP=(
  birth-death-services
  pqm
  pqm-anomaly-finder
  pt-services-v2
  rainmaker-pgr
)

needs_skip() {
  local m="$1"
  for x in "${BOOT22_SKIP[@]}"; do
    [[ "$x" == "$m" ]] && return 0
  done
  return 1
}

mapfile -t MODULES < <(
  find "$ROOT" -mindepth 1 -maxdepth 1 -type d ! -name '.*' ! -name 'upyog-aggregation-service' \
    ! -name 'core-smoke-tests' \
    -exec test -f '{}/pom.xml' \; -print | xargs -I{} basename {} | sort
)

: >"$LOG"
OK=0
FAIL=0
FAIL_LIST=()

for m in "${MODULES[@]}"; do
  echo "=== $m ===" | tee -a "$LOG"
  EXTRA=""
  if needs_skip "$m"; then
    EXTRA="-Dspring-boot.repackage.skip=true"
  fi
  if mvn -f "$ROOT/$m/pom.xml" $SETTINGS clean verify -q $EXTRA >>"$LOG" 2>&1; then
    echo "OK $m" | tee -a "$LOG"
    OK=$((OK + 1))
  else
    echo "FAIL $m" | tee -a "$LOG"
    FAIL=$((FAIL + 1))
    FAIL_LIST+=("$m")
  fi
done

echo "SUMMARY OK=$OK FAIL=$FAIL" | tee -a "$LOG"
if ((${#FAIL_LIST[@]})); then
  echo "FAILED: ${FAIL_LIST[*]}" | tee -a "$LOG"
fi
echo "DONE" | tee -a "$LOG"
