# egov-pg-service

Existing tests are JUnit 4. Surefire on Spring Boot 3 does not discover them without the vintage engine, so the build adds test-scoped JUnit Jupiter 5.9.3, Mockito, AssertJ, and `junit-vintage-engine`. `*IT` classes are excluded. JaCoCo enforces 0.10 line coverage on the bundle at verify.

```bash
mvn -q test
```

Integration tests (Spring context and Testcontainers) are deferred.
