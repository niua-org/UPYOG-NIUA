# Gateway tests

Unit tests live in `src/test/java` and run on JUnit 5 through `spring-boot-starter-test` (Mockito and AssertJ come with that starter). They do not boot the application or call a real user service.

From the `gateway` directory:

```bash
mvn test
```

`mvn -q test` is the same suite with quieter logging.

Integration tests are deferred: this module has no `*IT` classes, and the Failsafe plugin is declared without a suite to run.
