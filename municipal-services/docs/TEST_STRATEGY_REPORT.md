# Municipal services test strategy report

Branch: `cursor/municipal-services-test-strategy-572c` → `niua-dev-3.0`  
PR: #1940

## Scope completed

All deployable modules under `municipal-services/` now have:

- Maven **Surefire/Failsafe** split (`*Test` / `*IT`) for Java services, or **Jest** / **pytest** for Node/Python
- **JaCoCo** baseline gate (floor 1% line, raised over time toward 80%/70% branch on service/validator packages)
- **ArchUnit** layering test (controllers must not depend on repositories)
- **README-TESTING.md** per module
- **core-smoke-tests** registration (`services.yaml` + `@SmokeService` class) for HTTP deployables
- **Testcontainers** skeleton IT (`PostgresIntegrationIT`, enabled with `RUN_INTEGRATION_TESTS=true`)

Shared tooling: `municipal-services/testing/`, `.mvn/settings-public.xml` (Maven Central + UPYOG Nexus URLs).

## Module test run matrix (this agent run)

Command: `mvn -s municipal-services/.mvn/settings-public.xml test` on JDK 21, from `testing/run_module_tests.sh`.

| Result | Modules |
|--------|---------|
| **PASS (19)** | advertisement-service, asset-calculator, asset-services, bpa-calculator, challan-generation, challan-generation-calculator, cnd-calculator, community-hall-booking, estate-management, ewaste-services, fsm-calculator, garbage-service, ndc-calculator, ndc-services, pet-services, pgr-ai-services, **pgr-services**, pt-calculator-v2, vehicle |
| **FAIL (27)** | birth-death-services, bpa-services, cnd-service, echallan-calculator, echallan-services, egov-user-event, fsm, inbox, land-services, noc-services, pqm, pqm-anomaly-finder, property-services, pt-services-v2, rainmaker-pgr, request-service, street-vending, sw-calculator, sw-services, tl-calculator, tl-services, tp-services, turn-io-adapter, vendor, vendor-management, ws-calculator, ws-services |
| **Node PASS** | firenoc-services (`npm run test:unit`), firenoc-calculator (`npm run test:unit`) |
| **Python PASS** | pqm-scheduler (`pytest tests/`) |
| **Smoke compile** | core-smoke-tests (`mvn test-compile`) |
| **Go** | upyog-aggregation-service (existing `go test ./...` — not re-run in this sweep) |

Failure buckets (no production fixes applied):

1. **Lombok / JDK 21 compile** — e.g. `ws-services`, `property-services`, `vendor-management` (`JCTree` / annotation processing).
2. **Existing unit tests + new ArchUnit/IT** — compilation or context errors in legacy tests (rainmaker-pgr, pt-services-v2, request-service, …).
3. **Strict validation / missing mocks** — controller or validator tests need additional fixtures (documented under Findings).

Re-run locally with JDK 17 (service POM parent target) and corporate Nexus for best parity with CI.

## Coverage before vs after (qualitative)

| Module group | Before | After |
|--------------|--------|-------|
| All 46 Java Maven modules | 0–6 tests each; rarely JaCoCo/Failsafe | +ArchUnit, +unit target test, +IT skeleton, +JaCoCo gate, +README |
| P0 (property, pgr, ws) | Minimal | pgr **PASS** full suite; property/ws tests added but **compile blocked on JDK 21** |
| community-hall-booking | 23 tests | **PASS** + Failsafe split + gate |
| vendor-management | 6 tests | Tests added; **FAIL** in JDK 21 sweep (Lombok compile) |
| Node firenoc-* | eslint only | Jest unit scripts + tests |
| core-smoke-tests | 11 services | **50+** services in `services.yaml` + smoke classes |

Exact JaCoCo percentages: run `mvn test jacoco:report` per module in CI.

## Phase 3 extras (status)

| Type | Status |
|------|--------|
| ArchUnit | All Java modules |
| OWASP `-Powasp` profile | All Java modules (from generator) |
| Testcontainers IT skeleton | All Java modules (`RUN_INTEGRATION_TESTS=true`) |
| WireMock | Declared test dependency; scenario tests deferred |
| Pact / Kafka schema / migration-with-data | Deferred |
| PIT / Stryker / k6 | Deferred |

## Findings (production unchanged)

1. **pgr-services `ServiceRequestValidator.validateCreate`** — null or incomplete `service` can NPE before aggregated `CustomException`.
2. **pgr-services `RequestsApiController.requestsCreatePost`** — NPE if `pgrService.create` returns null (no guard).
3. **vendor-management** — duplicate constructor params; MDMS validator TODO; hard-coded `resMsgId`.
4. **JDK 21 + Lombok** — several modules fail to compile with the agent VM JDK; use JDK 17 in CI (as POM parent specifies).
5. **Tenant isolation** — smoke tests add black-box checks; repository-level isolation not proven for every API.
6. **Generated controller tests** — many modules rely on standalone MockMvc or util/validator tests first; expand `@WebMvcTest` with service-specific `@MockBean`s over time.

## Small refactors for testability

None in `src/main/java` — tests use Mockito, standalone MockMvc, and Testcontainers skeletons only.

## How to run

See each module’s `README-TESTING.md` and `municipal-services/TESTING.md`.

```bash
export MAVEN_OPTS=""
mvn -s municipal-services/.mvn/settings-public.xml test
cd municipal-services/core-smoke-tests && mvn -Psmoke -Dservice=ALL test
```
