# egf-instrument tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/egf-instrument
mvn test
mvn verify        # includes EgfInstrumentSchemaIT when Docker is available
```

JaCoCo report: `target/site/jacoco/index.html`

Surefire runs domain and mapper unit tests. Legacy `@SpringBootTest` JDBC repository specs under `persistence/repository/*JdbcRepositoryTest` and `web/**` are excluded until slice/H2 refresh (see `pom.xml` surefire excludes).
