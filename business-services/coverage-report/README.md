# Coverage pass artifacts

Reports in this folder are generated during the business-services behavioural coverage pass.

Helper scripts (repo root `.gitignore` excludes paths named `coverage/`):

- `business-services/scripts/coverage/jacoco-analyze.py` → `baseline.md`
- `business-services/scripts/coverage/find-shallow-tests.py` → `shallow-tests.md`
- `business-services/scripts/coverage/module-summary-from-jacoco.py` → service-layer module table

```bash
export JAVA_HOME=/tmp/jdk-17
cd business-services
for m in */; do (cd "$m" && mvn clean test jacoco:report -DskipITs=true -q); done
python3 scripts/coverage/jacoco-analyze.py . --out-dir coverage-report
python3 scripts/coverage/module-summary-from-jacoco.py
```
