# dashboard-analytics tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/dashboard-analytics
mvn test
mvn verify
```

JaCoCo report: `target/site/jacoco/index.html`
