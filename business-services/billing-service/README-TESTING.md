# billing-service tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/billing-service
mvn test          # unit tests (*Test.java)
mvn verify        # unit + BillingSchemaIT (Docker required)
```

JaCoCo report: `target/site/jacoco/index.html`

Integration tests use Testcontainers Postgres and are skipped when Docker is unavailable.
