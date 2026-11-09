# Remaining services — test findings

Recorded 2026-10-21. Commands used JDK 17.0.10 (`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`). The default Maven JDK on this machine is 23, and Lombok 1.18.32 does not generate accessors on that JDK. No git commit.

## tenant

Command: `mvn -q test` (after removing `target/` left by an earlier JDK 23 compile that wrote class files without Lombok methods).

Exit code: 1.

```
[ERROR] Tests run: 56, Failures: 6, Errors: 11, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.2:test (default-test) on project tenant: There are test failures.
```

`TenantServiceUnitTest` compiled and passed: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0.

Other passing sets: `TenantServiceTest` 5, `CityTest` (domain) 3, `TenantTest` (domain) 11, `CityRowMapperTest` 1, `TenantRowMapperTest` 1, `TenantTest` (entity) 1, `CityTest` (contract) 2, `TenantTest` (contract) 3.

Failures and errors (existing tests, production code unchanged):

- `TenantControllerTest` — 6 failures. `test_should_create_tenant` and `test_should_update_tenant`: status expected 200 but was 400. The four error-response tests expected JSON paths `error` and `responseInfo` and found neither.
- `TenantCreateRequestErrorAdapterTest` — 1 error, `UnnecessaryStubbing` at `test_should_set_error_when_city_name_is_absent` (line 139) and `test_should_set_error_when_ulb_grade_is_absent` (line 156).
- `EgovTenantApplicationTests.contextLoads`, `CityRepositoryTest` (3), and `TenantRepositoryTest` (6) — `Failed to load ApplicationContext`. Root cause: `org.flywaydb.core.api.FlywayException: Found more than one migration with version 20170426173457`. Offenders: `db/migration/qa/V20170426173457__insert_tenant_data.sql` and `db/migration/dev/V20170426173457__insert_tenant_data.sql`.

## egov-pg-service

Existing tests are JUnit 4, so Surefire on Spring Boot 3.2.2 would not run them. Added test-scoped `junit-jupiter` 5.9.3, `junit-vintage-engine`, `mockito-core`, `mockito-junit-jupiter`, and `assertj-core`, plus Surefire `**/*IT.java` exclude and JaCoCo bundle line minimum 0.10 (check goal, verify phase). Existing tests were not rewritten. `TESTING.md` added.

Command: `mvn -q test` (once).

Exit code: 1.

```
[ERROR] Tests run: 31, Failures: 0, Errors: 6, Skipped: 10
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project egov-pg-service:
```

Passed: `TransactionServiceTest` 8, `TransactionValidatorTest` 7.

Skipped (`@Ignore`): `IdGenRepositoryTest` 1, `CollectionsRepositoryTest` 1, default-package `Test` 8.

Errors: `TransactionsApiControllerTest` 6/6. `Failed to load ApplicationContext` for `@WebMvcTest`. Cause:

```
org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'org.egov.pg.service.RefundService' available
```

`refundController` requires `RefundService` and the slice does not provide that bean. Repeated methods then report `ApplicationContext failure threshold (1) exceeded`.

## Not started

- egov-document-uploader
- service-request
- national-dashboard-kafka-pipeline
- upyog-dpdp-service
- nlp-engine
- xstate-chatbot
