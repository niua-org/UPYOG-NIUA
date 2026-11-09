# egov-user testing

Unit tests cover `UserService` and the `UserController` create/search methods with JUnit 5, Mockito, and AssertJ. Repositories, encryption, OTP, and password encoding are mocked. The Spring context is not started.

## Run

```
mvn -q test
```

Surefire excludes `*IT`. JaCoCo checks a 0.10 line minimum on `org.egov.user.domain.service` during `test`.

## Deferred integration tests

Integration tests are deferred because the user service is large and the application context is not booted.
