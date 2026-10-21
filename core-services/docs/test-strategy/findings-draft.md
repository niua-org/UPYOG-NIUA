# upyog-draft-service findings

Command (from `upyog-draft-service`):

```bash
mvn -q test
```

Result: **failed**, exit code 1. Surefire 3.1.2. `DraftServiceTest` compiled. JaCoCo's 0.10 line check on `org.upyog.draft.service` did not run, because Surefire failed the build first.

```
Tests run: 21, Failures: 0, Errors: 21, Skipped: 0, Time elapsed: 19.75 s
```

Every error is the same and happens in `MockitoExtension.beforeEach` while mocking `org.upyog.draft.repository.DraftRepository`. No test body ran, so this run did not confirm or reject production behavior.

JVM that executed the tests: Homebrew OpenJDK 23.0.2 (`Java 23`). The module compiler source and target stay at 17.

Cause, from the Surefire output:

```
java.lang.IllegalArgumentException: Java 23 (67) is not supported by the current version of Byte Buddy which officially supports Java 22 (66) - update Byte Buddy or set net.bytebuddy.experimental as a VM property
```

Mockito then reports `Could not modify all classes [class java.lang.Object, class org.upyog.draft.repository.DraftRepository]`. A suppressed `NullPointerException` in `MockitoExtension.afterEach` follows that setup failure.

`DraftServiceTest` was not changed. It did not fail to compile. Re-run `mvn -q test` on a Java 17 JDK (the compiler target) to get past the Byte Buddy limit. Do not change the compiler target to match Java 23.
