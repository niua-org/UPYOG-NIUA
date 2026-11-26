#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/tmp/jdk-17}"
export PATH="${JAVA_HOME}/bin:${PATH:-}"
MVN="${MAVEN_HOME:-/tmp/apache-maven-3.9.6}/bin/mvn"
exec "$MVN" "$@"
