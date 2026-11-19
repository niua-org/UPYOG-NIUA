# dashboard-data-extractor tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
mvn -f business-services/dashboard-data-engine/pom.xml install -DskipTests
cd business-services/dashboard-data-extractor
mvn test
mvn verify        # Failsafe *IT tests (flow + Flyway schema when Docker is available)
```

JaCoCo report: `target/site/jacoco/index.html`
