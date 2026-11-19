# collection-services tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/collection-services
mvn test          # unit tests (*Test.java)
mvn verify        # unit + *IT integration (Docker required)
```

JaCoCo report: `target/site/jacoco/index.html`

Integration tests use Testcontainers Postgres and are skipped when Docker is unavailable.
