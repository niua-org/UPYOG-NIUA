# vendor-management — testing

## Unit tests (fast)

```bash
mvn test
```

Naming: `*Test.java`, pattern `methodName_condition_expectedResult`.

## Integration tests

```bash
mvn verify -DskipTests
```

Runs `*IT.java` via Failsafe (Testcontainers when present).

## Coverage gate

JaCoCo thresholds on `org.egov.vendor.service` and `org.egov.vendor.utils` (baseline gates in `pom.xml`; raise toward 80% / 70% branch over time).

## Dependency scan (CI)

```bash
mvn verify -Powasp
```

## Post-deploy smoke

Shared job: `municipal-services/core-smoke-tests` with `SERVICE=vendor-management`.
