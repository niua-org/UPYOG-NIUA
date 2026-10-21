#!/usr/bin/env bash
# Run mvn test + jacoco report for one module; print line/branch coverage from jacoco.csv if present.
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="/tmp/apache-maven-3.9.6/bin:${JAVA_HOME}/bin:${PATH}"
MODULE="${1:?module}"
POM="/workspace/municipal-services/${MODULE}/pom.xml"
SETTINGS="-s /workspace/municipal-services/.mvn/settings-public.xml"
mvn -f "$POM" $SETTINGS -q test jacoco:report
CSV="/workspace/municipal-services/${MODULE}/target/site/jacoco/jacoco.csv"
if [[ -f "$CSV" ]]; then
  python3 - "$CSV" <<'PY'
import csv, sys
path = sys.argv[1]
line_cov = branch_cov = 0.0
with open(path) as f:
    for row in csv.DictReader(f):
        if row.get("PACKAGE") == "all":
            line_cov = float(row["LINE_COVERED"]) / max(1, float(row["LINE_MISSED"]) + float(row["LINE_COVERED"]))
            bc = float(row["BRANCH_COVERED"])
            bm = float(row["BRANCH_MISSED"])
            branch_cov = bc / max(1, bc + bm)
print(f"LINE={line_cov:.4f} BRANCH={branch_cov:.4f}")
PY
fi
