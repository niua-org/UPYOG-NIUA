# egf-master tests

## Commands

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
cd business-services/egf-master
mvn test          # unit tests (*Test.java)
mvn verify        # unit + EgfMasterSchemaIT (Docker required)
```

JaCoCo report: `target/site/jacoco/index.html`

Schema integration tests migrate Flyway SQL against Testcontainers Postgres without starting Elasticsearch.

`mvn test` runs domain and service unit tests; legacy `web/controller` MockMvc specs are excluded until slice configuration is updated.
