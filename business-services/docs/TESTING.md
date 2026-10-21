# business-services testing conventions

## Layers

| Layer | Naming | Maven phase | Command |
|-------|--------|-------------|---------|
| Unit | `*Test.java` | `test` (Surefire) | `mvn test` |
| Integration | `*IT.java` | `verify` (Failsafe) | `mvn verify` |
| Post-deploy smoke | `@Tag("smoke")` in `core-smoke-tests` | profile `smoke` | `mvn -Psmoke verify` |

## Stack

- JUnit 5, Mockito, AssertJ (`spring-boot-starter-test`)
- Testcontainers + shared helpers (`business-services-test-support`)
- WireMock for MDMS / user / workflow / idgen stubs
- JaCoCo line/branch gates on service and validator packages (DTOs/config excluded)

## Local integration tests

Requires Docker. Install test-support once:

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
```

Run a service:

```bash
cd business-services/<service>
mvn test          # unit only
mvn verify        # unit + *IT integration
```

## CI

Service pipelines run `mvn verify`, publish JaCoCo, OWASP Dependency-Check (where configured), then deploy and trigger `core-smoke-tests` with `SERVICE=<name>`.
