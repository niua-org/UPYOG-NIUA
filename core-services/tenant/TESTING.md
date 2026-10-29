# tenant

Unit tests for `TenantService` use JUnit 5, `MockitoExtension`, and AssertJ. They construct the service directly, mock repositories, and do not start Spring or open a network connection.

```bash
mvn -Dtest=TenantServiceUnitTest test
```

Integration tests (Spring context and Testcontainers) are deferred.
