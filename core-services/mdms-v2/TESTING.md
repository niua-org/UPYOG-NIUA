# mdms-v2 tests

Unit tests live in `src/test/java`. They use JUnit Jupiter 5.9.3, Mockito, and AssertJ. They do not start Spring and they do not call the network.

The compiler source and target stay at 17. JUnit 5.9.3 is the Java 8-compatible Jupiter line requested for this module.

## Commands

From `mdms-v2`:

- `mvn test` — Surefire runs `*Test` and excludes `*IT`.
- `mvn verify` — Surefire, then Failsafe (`*IT` only), then the JaCoCo check.

JaCoCo's line-coverage minimum is 0.10 for `MDMSService` and `org.egov.infra.mdms.service.validator`. Packages `model` and `config` are excluded from the JaCoCo report. The check is bound to `verify`, so `mvn test` does not fail the build on coverage.

## Integration tests

No `*IT` is checked in. Flyway is on the classpath, but a bootable Spring context and database were not confirmed, so an end-to-end test is deferred. `mvn verify` has no Failsafe tests to run.

## ArchUnit

Not added. `MDMSController` delegates to `MDMSService` and does not call a repository. Other controllers were not inspected, so a module-wide layering rule was not introduced.
