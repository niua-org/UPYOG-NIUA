# municipal-services testing conventions

## Layout

| Layer | Java | Node (firenoc-services) |
|-------|------|-------------------------|
| Unit | `*Test.java`, Surefire, `mvn test` | `test/unit`, `npm run test:unit` |
| Integration | `*IT.java`, Failsafe, `mvn verify` | `test/integration`, `npm run test:integration` |
| Post-deploy smoke | `core-smoke-tests`, `mvn -Psmoke test` | Same HTTP suite |

## Naming

`methodName_condition_expectedResult`

## Determinism

No thread sleeps in tests — use Awaitility. Inject clocks where applicable.

## Coverage

JaCoCo gates on service/validator packages (baseline per service in `pom.xml`; target 80% line / 70% branch). DTOs, config, and generated code excluded from gates.

## CI

Per-service pipeline: unit → integration (`verify`) → JaCoCo gate → OWASP `-Powasp` → deploy → trigger `core-smoke-tests` (see `jenkins/deploy-with-smoke.groovy`).

## Adding tests to a new service

1. Copy Surefire/Failsafe/JaCoCo block from `vendor-management/pom.xml`.
2. Add `README-TESTING.md`.
3. Register in `core-smoke-tests/src/test/resources/services.yaml` + smoke class.

See `docs/TEST_STRATEGY_REPORT.md` for coverage status and deferred services.
