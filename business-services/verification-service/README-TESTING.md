# verification-service tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/verification-service
mvn test          # unit tests (*Test.java)
mvn verify        # unit + VerificationServiceContextIT (Docker required)
```

JaCoCo report: `target/site/jacoco/index.html`

Integration tests use Testcontainers Postgres for datasource wiring and are skipped when Docker is unavailable.
