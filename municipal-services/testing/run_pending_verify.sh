#!/usr/bin/env bash
set -uo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="/tmp/apache-maven-3.9.6/bin:${JAVA_HOME}/bin:${PATH}"
SETTINGS="-s /workspace/municipal-services/.mvn/settings-public.xml"
MODULES=(
  advertisement-service asset-calculator asset-services birth-death-services challan-generation
  cnd-service echallan-services egov-user-event estate-management ewaste-services land-services
  pgr-ai-services pqm pqm-anomaly-finder pt-services-v2 tp-services turn-io-adapter vehicle vendor-management
)
for m in "${MODULES[@]}"; do
  echo "==== $m ===="
  if mvn -f "/workspace/municipal-services/${m}/pom.xml" $SETTINGS clean verify -q; then
    python3 /workspace/municipal-services/testing/update_gate_floor.py "$m" || true
    python3 /workspace/municipal-services/testing/record_module_coverage.py "$m"
    echo "PASS $m"
  else
    echo "FAIL $m"
  fi
done
