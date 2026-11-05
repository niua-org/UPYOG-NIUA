# dashboard-ingest tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/dashboard-ingest
mvn test          # unit tests (*Test.java)
mvn verify        # unit + IngestAppContextIT
```

JaCoCo report: `target/site/jacoco/index.html`

Integration tests load the Spring context with Kafka auto-start disabled.
