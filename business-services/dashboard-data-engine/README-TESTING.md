# dashboard-data-engine tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/dashboard-data-engine
mvn test
mvn verify
```

- Unit tests: Surefire (`*Test.java`, excluding `*IT.java`).
- Integration tests: Failsafe (`*IT.java`) when present.
- ArchUnit pilot: `LayeringArchitectureTest` enforces basic package layering.
- JaCoCo report: `target/site/jacoco/index.html`
