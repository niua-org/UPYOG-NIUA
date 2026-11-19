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

## JDK 17 (required for tests)

Install Temurin 17 locally or use the repo helper (Cloud Agent: `/tmp/jdk-17`):

```bash
export JAVA_HOME=/tmp/jdk-17   # or your JDK 17 home
business-services/scripts/mvn-java17.sh -f business-services/<service>/pom.xml test
```

Maven Toolchains: `~/.m2/toolchains.xml` may point `jdkHome` at JDK 17. **Production bytecode** stays at each module’s configured `release` (e.g. reconciliation-service remains **11**; egf modules may remain **8**). Tests compile and run on **JDK 17**.

## Local integration tests

Requires Docker. Install test-support once:

```bash
business-services/scripts/mvn-java17.sh -f business-services/test-support/pom.xml install -DskipTests
```

Run a service:

```bash
cd business-services/<service>
business-services/scripts/mvn-java17.sh test          # unit only
business-services/scripts/mvn-java17.sh verify        # unit + *IT integration
```

## CI

Service pipelines run `mvn verify`, publish JaCoCo, OWASP Dependency-Check (where configured), then deploy and trigger `core-smoke-tests` with `SERVICE=<name>`.
