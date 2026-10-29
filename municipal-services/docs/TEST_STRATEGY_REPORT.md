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

## Per-class unit test expansion (JDK 17, complete)

Tooling: [`testing/generate_per_class_unit_tests.py`](../testing/generate_per_class_unit_tests.py), [`testing/generate_node_unit_tests.py`](../testing/generate_node_unit_tests.py), [`testing/audit_module_coverage.py`](../testing/audit_module_coverage.py), [`testing/update_jacoco_floors.py`](../testing/update_jacoco_floors.py).

Inventory: [`module-coverage-audit.json`](../testing/module-coverage-audit.json) — **46/46** modules **PASS** `mvn test` on JDK 17 after per-class scaffolding; JaCoCo measured via full-tree `mvn test jacoco:report` (2026-10-21). POM floors set to ~95% of measured line/branch via `update_jacoco_floors.py`.

Remaining **naming gaps** (hand-written or FQN tests; no production change): `pqm*` types named `Test*`, `tp-services` inner `State`, HRMS `DepartmentalTest` in `vehicle`/`vendor`, `pt-calculator-v2` `ReceiptService` (entire source file commented out in main — no test class).

| Module | Main classes | With `FooTest` | Missing | Test classes | mvn test | Line % | Branch % |
|--------|-------------:|---------------:|--------:|-------------:|----------|-------:|---------:|
| advertisement-service | 102 | 102 | — | 103 | **PASS** | 16.5% | 0.7% |
| asset-calculator | 36 | 36 | — | 37 | **PASS** | 22.2% | 0.0% |
| asset-services | 84 | 84 | — | 86 | **PASS** | 22.6% | 0.0% |
| birth-death-services | 104 | 104 | — | 105 | **PASS** | 7.6% | 0.4% |
| bpa-calculator | 54 | 54 | — | 55 | **PASS** | 13.4% | 0.2% |
| bpa-services | 114 | 114 | — | 115 | **PASS** | 6.5% | 0.0% |
| challan-generation | 88 | 88 | — | 89 | **PASS** | 13.3% | 0.1% |
| challan-generation-calculator | 43 | 43 | — | 44 | **PASS** | 11.2% | 0.3% |
| cnd-calculator | 32 | 32 | — | 33 | **PASS** | 30.1% | 0.5% |
| cnd-service | 86 | 86 | — | 88 | **PASS** | 14.9% | 0.1% |
| community-hall-booking | 123 | 123 | — | 125 | **PASS** | 33.1% | 8.5% |
| echallan-calculator | 43 | 43 | — | 44 | **PASS** | 15.4% | 0.4% |
| echallan-services | 75 | 75 | — | 76 | **PASS** | 15.5% | 0.6% |
| egov-user-event | 35 | 35 | — | 36 | **PASS** | 9.3% | 0.9% |
| estate-management | 77 | 77 | — | 79 | **PASS** | 10.7% | 0.4% |
| ewaste-services | 72 | 72 | — | 73 | **PASS** | 15.2% | 1.1% |
| fsm | 130 | 130 | — | 131 | **PASS** | 7.8% | 0.0% |
| fsm-calculator | 57 | 57 | — | 58 | **PASS** | 19.7% | 0.3% |
| garbage-service | 109 | 109 | — | 110 | **PASS** | 13.0% | 0.6% |
| inbox | 140 | 140 | — | 141 | **PASS** | 4.9% | 0.3% |
| land-services | 44 | 44 | — | 45 | **PASS** | 12.1% | 0.0% |
| ndc-calculator | 56 | 56 | — | 57 | **PASS** | 17.2% | 0.4% |
| ndc-services | 114 | 114 | — | 115 | **PASS** | 9.4% | 0.1% |
| noc-services | 77 | 77 | — | 78 | **PASS** | 5.2% | 0.0% |
| pet-services | 99 | 99 | — | 100 | **PASS** | 11.3% | 0.2% |
| pgr-ai-services | 91 | 91 | — | 92 | **PASS** | 13.4% | 0.1% |
| pgr-services | 84 | 84 | — | 85 | **PASS** | 17.6% | 0.9% |
| pqm | 95 | 84 | MdmsTest, Test, TestDocumentQueryBuilder, TestQueryBuilder… | 96 | **PASS** | 13.1% | 0.4% |
| pqm-anomaly-finder | 53 | 48 | Test, TestAnomalyFinderValidator, TestRequest, TestResponse… | 54 | **PASS** | 19.4% | 0.9% |
| property-services | 171 | 171 | — | 173 | **PASS** | 8.0% | 0.3% |
| pt-calculator-v2 | 146 | 145 | ReceiptService | 146 | **PASS** | 12.8% | 0.4% |
| pt-services-v2 | 92 | 92 | — | 94 | **PASS** | 12.0% | 0.4% |
| rainmaker-pgr | 68 | 68 | — | 71 | **PASS** | 8.7% | 1.9% |
| request-service | 100 | 100 | — | 102 | **PASS** | 11.7% | 0.5% |
| street-vending | 119 | 119 | — | 121 | **PASS** | 19.7% | 0.4% |
| sw-calculator | 129 | 129 | — | 130 | **PASS** | 11.1% | 0.5% |
| sw-services | 123 | 123 | — | 124 | **PASS** | 11.3% | 0.5% |
| tl-calculator | 76 | 76 | — | 78 | **PASS** | 19.4% | 0.8% |
| tl-services | 107 | 107 | — | 108 | **PASS** | 13.1% | 0.3% |
| tp-services | 91 | 90 | State | 92 | **PASS** | 22.0% | 0.7% |
| turn-io-adapter | 65 | 65 | — | 66 | **PASS** | 13.5% | 0.2% |
| vehicle | 85 | 84 | DepartmentalTest | 86 | **PASS** | 10.2% | 0.1% |
| vendor | 65 | 64 | DepartmentalTest | 66 | **PASS** | 9.1% | 0.1% |
| vendor-management | 42 | 42 | — | 43 | **PASS** | 33.4% | 1.7% |
| ws-calculator | 144 | 144 | — | 145 | **PASS** | 10.3% | 0.5% |
| ws-services | 131 | 131 | — | 133 | **PASS** | 11.5% | 0.6% |

Node/Python: **firenoc-services** / **firenoc-calculator** — Jest per-`src` smoke tests (20 + 18 files, entry/kafka/db skipped); **pqm-scheduler** — pytest for `cronJobAPIConfig.getRequestInfo`.

## Coverage before vs after (qualitative)

| Module group | Before | After |
|--------------|--------|-------|
| All 46 Java Maven modules | 0–6 tests each; rarely JaCoCo/Failsafe | +ArchUnit, +unit target test, +IT skeleton, +JaCoCo gate, +README |
| P0 (property, pgr, ws) | Minimal | **PASS** on JDK 17 full `mvn test` |
| community-hall-booking | 23 tests | **PASS** + Failsafe split + gate |
| vendor-management | 6 tests | **PASS** on JDK 17 |
| Node firenoc-* | eslint only | Jest unit scripts + tests |
| core-smoke-tests | 11 services | **50+** services in `services.yaml` + smoke classes |

JaCoCo percentages in the table above come from the 2026-10-21 full-tree run; CI should repeat `mvn test jacoco:report` on JDK 17.

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
4. **JDK 21 + Lombok** — prior agent sweep failed compile on several modules; **JDK 17** is required for parity with POM targets (all 46 modules pass on 17).
5. **Legacy layering** — `InboxV2Controller`, `DemandBasedController`, `PqmAnomalyFinderController` inject repository/query-builder types directly; ArchUnit rules exclude these known paths until refactor.
6. **GlobalExceptionHandler (street-vending, tp-services)** — standalone MockMvc tests observe **500** for some validation/custom paths where handlers map to generic error responses (tests assert observed behavior).
7. **Tenant isolation** — smoke tests add black-box checks; repository-level isolation not proven for every API.
8. **Generated controller tests** — many modules rely on standalone MockMvc or util/validator tests first; expand `@WebMvcTest` with service-specific `@MockBean`s over time.
9. **Per-class generator** — `@Mock` fields keep generic types (`Map<String, Object>`); FQN constructor params resolve imports; `String` / `@Value` constructor params are literals, not mocks; model class `Test` uses FQN to avoid clashing with JUnit `@Test`; `@Builder` without `@NoArgsConstructor` gets builder-only tests.
10. **pt-calculator-v2 `ReceiptService`** — production class is fully block-commented; per-class inventory skips a test for that type.
11. **LocalizationConstants.EMPTY_DOCUMENT_ERROR** — constant is `null` in production; generated constants test asserts null, not blank.

## Small refactors for testability

None in `src/main/java` — tests use Mockito, standalone MockMvc, and Testcontainers skeletons only.

## How to run

See each module’s `README-TESTING.md` and `municipal-services/TESTING.md`.

```bash
export MAVEN_OPTS=""
mvn -s municipal-services/.mvn/settings-public.xml test
cd municipal-services/core-smoke-tests && mvn -Psmoke -Dservice=ALL test
```
