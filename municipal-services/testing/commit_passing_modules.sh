#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="/tmp/apache-maven-3.9.6/bin:${JAVA_HOME}/bin:${PATH}"
SETTINGS="-s /workspace/municipal-services/.mvn/settings-public.xml"
ROOT="/workspace"
cd "$ROOT"
for pom in /workspace/municipal-services/*/pom.xml; do
  module=$(basename "$(dirname "$pom")")
  [[ "$module" == "core-smoke-tests" ]] && continue
  if ! mvn -f "$pom" $SETTINGS -q test >/dev/null 2>&1; then
    continue
  fi
  git add "municipal-services/${module}"
  if git diff --cached --quiet; then
    continue
  fi
  git commit -m "tests: per-class unit test scaffolding for ${module} (JDK 17)"
  echo "committed ${module}"
done
