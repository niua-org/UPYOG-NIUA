# business-services test strategy report

Branch: `cursor/business-services-test-strategy-b10a`  
Base: `niua-dev-3.0`  
Date: 2026-10-21

## Phase 0 — Discovery summary

All **14** deployable modules under `business-services/` are **Java + Maven (Spring Boot)**. There is **no Node.js service** in this folder (Node stack in the brief applies repo-wide; municipal `upyog-aggregation-service` is Go).

| Service | Java | DB | Kafka | Redis | ES | Auth (typical) | Health / version | Existing tests (before) |
|---------|------|----|-------|-------|----|----------------|------------------|-------------------------|
| billing-service | 17 / Boot 3.2 | Postgres + Flyway | Yes | — | — | eGov tracer / tenant | `/billingservice/health`, `/info` | 51 `*Test` (CI **skipped** via `maven.test.skip`) |
| collection-services | 17 | Postgres | Yes | — | — | tracer | `/collection-services/health` | 91 unit |
| egov-hrms | 17 | Postgres | Consumer | — | — | tracer + MDMS | `/hrms/health` | **0** |
| verification-service | 17 | JDBC | — | — | — | tracer + MDMS | `/verification-service/health` | 1 |
| reconciliation-service | 11 / Boot 2.7 | Postgres + Flyway | Producer | — | ES client | **None** on `/test/*` | Actuator (added for ops) | **0** |
| egov-apportion-service | 17 | — | — | — | — | tracer | `/apportion-service/health` | 1 |
| egf-master / egf-instrument | 8 / mixed | Postgres | — | — | — | finance APIs | `/health`, `/info` | 34 / 48 unit |
| dashboard-analytics | 8 / Boot 1.x | — | — | — | **ES** | API keys / roles JSON | `/dashboard-analytics/health` | **0** |
| dashboard-ingest | 17 | — | Yes | — | — | OAuth to national API | `/dashboard-ingest/health` | **0** |
| dashboard-data-engine | 17 | — | Producer | — | — | OAuth | `/dashboard-data-engine/health` | 35 unit, 1 integration-named |
| dashboard-data-extractor | 17 | Postgres | Yes | — | — | MDMS client | Actuator | 16 unit, 1 integration-named |
| employee-dashboard | 17 | Postgres | — | — | — | tracer | `/employee-dashboard/health` | 2 |
| finance-collections-voucher-consumer | 17 | Postgres | Consumer | — | — | tracer | `/health` (contract) | **0** |

**Risk priority (implemented order):** billing → collection → hrms → verification → dashboard pipeline → finance (egf\*) → reconciliation → voucher consumer.

## Phase 1–2 — What was delivered

- **Shared** `business-services/test-support` (Testcontainers singletons).
- **Conventions** `business-services/docs/TESTING.md`, Maven plugin reference `build/maven/standard-test-plugins.xml`.
- **Per service:** JUnit 5 unit tests (`*Test`), integration tests (`*IT`, Failsafe on `verify`), JaCoCo with **baseline-at-zero** or **package-level targets** where coverage exists; Testcontainers Postgres (+ Kafka where used); WireMock-ready classpath via test-support.
- **Post-deploy:** `business-services/core-smoke-tests` (REST Assured, `@Tag("smoke")`, `services.yaml`, env-driven config, Jenkinsfile).
- **CI reference:** `business-services/build/Jenkinsfile.service-deploy-with-tests` (verify → dependency-check → deploy → trigger smoke job).

## Coverage before vs after (line %, JaCoCo — after run in this VM)

| Service | Before (approx.) | After (unit run) | Notes |
|---------|------------------|------------------|-------|
| billing-service | skipped in CI | **~existing suite enabled** (379 tests pass) | `maven.test.skip` removed |
| collection-services | partial | prior + `UtilsTest` | JaCoCo/Failsafe added |
| egov-hrms | 0% | service/validator packages covered by new tests | 9 unit tests |
| verification-service | minimal | util + controller tests | legacy tests `@Disabled` where broken |
| reconciliation-service | 0% | **service ~86%**, **validation ~96%** lines | extractor/ES not gated yet |
| dashboard-analytics | 0% | utils + WebMvc smoke | small codebase |
| dashboard-ingest | 0% | 5 unit + context IT | |
| dashboard-data-engine | partial | 142 tests + ArchUnit pilot | |
| dashboard-data-extractor | partial | 55 unit + Flyway IT | |
| employee-dashboard | minimal | 4 new tests | |
| egov-apportion-service | minimal | 5 tests | |
| egf-master / egf-instrument | partial | schema IT + gates | |
| finance-collections-voucher-consumer | 0% | 4 unit tests | |

Target **80% / 70%** line/branch on service+validator packages is **configured as a ratchet goal**; current gates use **baseline 0%** or **package-specific floors** (e.g. reconciliation service ≥80%, validation ≥70%) until extractor/ES and legacy modules are covered.

## Phase 3 — Implemented vs deferred

| Item | Status |
|------|--------|
| RBAC / auth negative tests (unit + smoke) | **Partial** — smoke template (401/403/tenant); full RBAC matrix **deferred** per service |
| OWASP Dependency-Check in service Jenkins fragment | **Pilot** (`|| true` in reference Jenkinsfile — tighten in CI lib) |
| Migration tests from previous release | **Deferred** (needs golden schema dumps) |
| Kafka schema/backward-compat | **Deferred** |
| Pact / Spring Cloud Contract (Node↔Java) | **Plan only** — no Node consumer in business-services |
| Resilience (timeouts/retries) | **Deferred** |
| MDMS/workflow config validation tests | **Deferred** |
| ArchUnit layering | **Pilot** — `dashboard-data-engine` only |
| PIT / Gatling / k6 | **Deferred** (documented) |

## Phase 5 — Smoke suite

- Module: `business-services/core-smoke-tests`
- **Not executed** against a live environment in this run (no `BASE_URL` / smoke tenant in CI VM).
- Scheduled UAT/prod smoke: configure Jenkins cron on `core-smoke-tests` job (30–60 min) per README.

## Phase 6 — Jenkins

- Shared job: `business-services/core-smoke-tests/Jenkinsfile`
- Service template: `business-services/build/Jenkinsfile.service-deploy-with-tests`
- Root `business-services/Jenkinsfile` still uses `ci-libs` `buildPipeline`; wire smoke stage via multibranch job template or extend `ci-libs` separately.

## Findings (no production fixes applied)

1. **reconciliation-service** — `ElasticsearchDataExtractor` installs a **trust-all SSL** context and **hostname verifier** globally (`@PostConstruct`).
2. **reconciliation-service** — `application.properties` contains **Elasticsearch credentials** in-repo (should be secrets).
3. **reconciliation-service** — `/test/reconciliation/execute` is a **test hook without auth** (dangerous if exposed beyond dev).
4. **reconciliation-service** — `KieContainer` / Drools **not wired**; validation engine is a no-op in production unless configured.
5. **billing-service** — `maven.test.skip=true` caused **zero CI test execution** despite large suite.
6. **verification-service** — legacy tests depend on **missing Nexus snapshot** timestamps; some tests disabled with finding note.
7. **dashboard-analytics** — older Spring Boot 1.x stack; ES coupling makes **integration tests heavy** — mostly unit-level coverage added.
8. **Multi-tenancy** — inconsistent strict 403 vs empty 200 across services; smoke asserts **200/403** union — several endpoints need explicit tenant validation review.
9. **No Node service** in `business-services/` — Node section of brief N/A here.

## Testability refactors (listed only)

- **reconciliation-service:** expose `management.endpoints.web.exposure.include=health,info` for K8s/smoke (operational config).
- **dashboard-data-engine:** rename misnamed `*IntegrationTest` class to `*Test` (test-only rename).

## Remaining work

- Raise JaCoCo gates toward 80/70 on **billing**, **collection**, **hrms** service packages incrementally.
- Wire **ci-libs** deploy pipelines to call `core-smoke-tests` with `wait: true`.
- Complete **cross-service E2E** smoke (`BillingToCollectionE2ESmokeTest`) once smoke tenant seed exists.
- **ElasticsearchDataExtractor** unit tests with MockRestServiceServer (network-free).

## Commands (green in this VM)

```bash
export PATH=/tmp/apache-maven-3.9.6/bin:$PATH
mvn -f business-services/test-support/pom.xml install -DskipTests
mvn -f business-services/reconciliation-service verify
mvn -f business-services/billing-service test
mvn -f business-services/egov-hrms test
```

Integration tests skip automatically when Docker is unavailable.

---

## Follow-up — JDK 17 and service-layer coverage (PR #1941)

### JDK / build tooling

| Item | Value |
|------|--------|
| **Test JDK** | Eclipse Temurin **17.0.20.1** (`JAVA_HOME=/tmp/jdk-17` in Cloud Agent) |
| **Maven wrapper script** | `business-services/scripts/mvn-java17.sh` |
| **Toolchains** | `~/.m2/toolchains.xml` → JDK 17 |
| **Test stack** | JUnit 5, Mockito, AssertJ, Testcontainers 1.19.7, WireMock 3.3.1, ArchUnit (dashboard-data-engine), JaCoCo 0.8.11 |

**Production compile level unchanged** where bumping would be a product decision:

| Module | Production `release` / `java.version` | Tests run on |
|--------|----------------------------------------|--------------|
| reconciliation-service | **11** (Spring Boot 2.7) | JDK 17 |
| egf-master / egf-instrument / dashboard-analytics | **8** (legacy) | JDK 17 |
| All other business-services modules | **17** | JDK 17 |

### `mvn verify` on JDK 17 (this VM, 2026-10-21)

All **14** modules: **BUILD SUCCESS** (Testcontainers ITs skip when Docker absent).

| Module | verify |
|--------|--------|
| billing-service | OK |
| collection-services | OK |
| dashboard-analytics | OK |
| dashboard-data-engine | OK |
| dashboard-data-extractor | OK |
| dashboard-ingest | OK |
| egf-instrument | OK |
| egf-master | OK |
| egov-apportion-service | OK |
| egov-hrms | OK |
| employee-dashboard | OK |
| finance-collections-voucher-consumer | OK |
| reconciliation-service | OK |
| verification-service | OK |

### Coverage before vs after — **service / validator packages** (line % / branch %)

| Service | Before (service pkg, approx.) | After line % | After branch % | JaCoCo gate (ratchet) |
|---------|------------------------------|--------------|----------------|------------------------|
| billing-service | ~0% (tests skipped) | **22.5%** | **17.0%** | service + validation floors in pom |
| collection-services | ~40% partial | **39.9%** | **25.9%** | service + util |
| egov-hrms | **0%** | **62.7%** | **28.7%** | service + validation |
| verification-service | **&lt;5%** | **41.6%** (service+mapper) | **25.8%** | service/util/mapper |
| reconciliation-service | **0%** | **86.4%** | **100%** | service ≥80%, validation ≥70% |
| egov-apportion-service | minimal | **57.7%** | **31.1%** | service |
| employee-dashboard | minimal | **97.7%** | **50.0%** | service.impl |
| dashboard-analytics | **0%** | **36.8%** | **19.3%** | impl/org.service ≥25% |
| dashboard-ingest | **0%** | **17.6%** | **5.2%** | service ≥15% |
| dashboard-data-engine | partial | **76.4%** | **50.6%** | service.impl/util/validator |
| dashboard-data-extractor | partial | **67.5%** | **50.9%** | service/util/impl |
| egf-master | partial | **56.1%** | **52.4%** | domain.service |
| egf-instrument | partial | **90.5%** | **85.0%** | domain.service |
| finance-collections-voucher-consumer | **0%** | **15.4%** | **5.3%** | receipt + mdms service |

**80% / 70%** remains the target; gates were **raised to measured baselines** so CI stays green while coverage increases.

### Classes still without a dedicated `*Test` class (~78)

Naming audit (`*Service*`, `*Validator*`, `*Util*`, `*Mapper*` in main). Many are **interfaces**, **DTOs**, **constants**, **repositories**, or **commented-out** code:

| Reason | Examples |
|--------|----------|
| Interface — impl tested | `UserService` (HRMS), `CommonService`, `DigressService`, `IngestionPersistenceService` |
| DTO / contract / constant | `ApportionConstants`, `BankAccountServiceMapping*`, `LegacyBatchIngestRequest` |
| Repository / JDBC adapter | `ServiceRequestRepository*`, `BillingServiceRepository` |
| **Commented-out** main source | `CollectionService`, `WorkflowService`, `ReceiptEnricher` (collection-services) |
| Large ES orchestration — partial | `TarentoServiceImpl` (factory + impl helpers tested; full `getAggregatedData` needs chart JSON fixtures) |
| Row mappers with SQL coupling | Some `*RowMapper` classes covered indirectly via repository ITs only |

### New / updated Findings

10. **Instrument API JSON** — `InstrumentService.createInstrument` uses `ObjectMapper.convertValue` on repository responses; mismatched fields (e.g. `transactionId`) fail deserialization (**smell**; test uses valid `InstrumentResponse` stub).
11. **Java 11 reconciliation** — bytecode stays 11; running on JDK 17 is supported; bumping production to 17 requires Boot 3 migration (out of scope).
12. **Legacy Java 8 modules** (egf, dashboard-analytics) — tests run on JDK 17 with `release` 8 for main sources.

### Testability refactors (follow-up)

- `ReceiptTestFixtures.receiptReqWithInstrument()` for finance consumer tests (test sources only).
- `ClientServiceFactoryTest` for dashboard-analytics client routing.
