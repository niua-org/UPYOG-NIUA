# reconciliation-service tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/reconciliation-service
mvn test          # unit tests (*Test.java)
mvn verify        # unit + ReconciliationSchemaIT (Docker required)
```

JaCoCo report: `target/site/jacoco/index.html`

Integration tests use Testcontainers Postgres and are skipped when Docker is unavailable.
