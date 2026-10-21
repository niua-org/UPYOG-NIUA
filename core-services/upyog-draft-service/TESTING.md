# upyog-draft-service testing

`DraftServiceTest` uses JUnit Jupiter, Mockito (`MockitoExtension`), and AssertJ. It constructs `DraftService` with a mocked `DraftRepository`. No Spring context is started and no network calls are made.

The module compiles for Java 17, so JUnit Jupiter, Mockito, and AssertJ versions come from the Spring Boot 3.2.2 BOM (not JUnit 5.9.3, which is for Java 8).

Run from `upyog-draft-service`:

```bash
mvn -q test
```

Surefire includes `**/*Test.java` and excludes `**/*IT.java`. JaCoCo checks a 0.10 line-coverage minimum on package `org.upyog.draft.service` during `mvn test`. Compiler source and target stay at 17.

Integration tests are deferred. A Testcontainers test against `V20260729100000__create_draft_detail.sql` was not added because Docker was not available in this environment.
