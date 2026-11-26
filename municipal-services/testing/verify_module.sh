#!/usr/bin/env bash
# test → jacoco report → align gate floors → verify
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="/tmp/apache-maven-3.9.6/bin:${JAVA_HOME}/bin:${PATH}"
MODULE="${1:?module}"
POM="/workspace/municipal-services/${MODULE}/pom.xml"
SETTINGS="-s /workspace/municipal-services/.mvn/settings-public.xml"
EXTRA="${2:-}"
mvn -f "$POM" $SETTINGS clean test jacoco:report -q $EXTRA
python3 /workspace/municipal-services/testing/update_gate_floor.py "$MODULE"
mvn -f "$POM" $SETTINGS verify -q $EXTRA
