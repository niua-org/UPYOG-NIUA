# business-services-test-support

Shared Testcontainers singletons and WireMock helpers for integration tests under `business-services/`.

## Usage

Add a test-scoped dependency in the service `pom.xml`:

```xml
<dependency>
  <groupId>org.upyog.business</groupId>
  <artifactId>business-services-test-support</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

Install locally once per workspace:

```bash
mvn -f business-services/test-support/pom.xml install -DskipTests
```

Integration tests require Docker. Tests are skipped automatically when Docker is unavailable via `@Testcontainers(disabledWithoutDocker = true)`.
