# Municipal services test strategy report

Branch: `feature/municipal-services-test-strategy` → `niua-dev-3.0`

## Phase 0 — discovery summary

**Stack:** 47 Maven (Spring Boot) modules, 1 Node (`firenoc-services`), 1 Go (`upyog-aggregation-service`), plus `firenoc-calculator` (Node migration sibling). Jenkins entry: `municipal-services/Jenkinsfile` → `build/build-config.yml`.

**Auth (typical):** Bearer token from `egov-user` OAuth; `RequestInfo.userInfo` + tenantId on APIs; employee vs citizen search rules enforced in validators.

**Infra (typical):** PostgreSQL + Flyway, Kafka (persister/workflow), Redis (select services e.g. CHB seat lock), Elasticsearch (inbox). External: MDMS, idgen, workflow, localization, HRMS, billing.

### Prioritized services (risk = downstream dependents × change frequency)

| Priority | Service | Lang | Existing tests | JaCoCo/c8 baseline | This PR |
|----------|---------|------|----------------|-------------------|---------|
| P0 | property-services | Java | 0 | ~0% | Unit starter + pom gates + smoke |
| P0 | pgr-services | Java | 2 (controller disabled) | ~0% | Validator tests + pom + smoke |
| P0 | ws-services | Java | 0 | ~0% | Smoke only (deferred deep tests) |
| P0 | sw-services | Java | 0 | ~0% | Deferred |
| P0 | bpa-services | Java | 0 | ~0% | Deferred |
| P1 | community-hall-booking | Java | 23 | partial (JaCoCo present) | Failsafe split, ArchUnit, gate |
| P1 | vendor-management | Java | 0 | ~0% | Full unit + ArchUnit + pom |
| P1 | firenoc-services | Node | eslint only | n/a | Jest unit + scripts |
| P1 | pt-services-v2 / pt-calculator-v2 | Java | few | low | Deferred |
| P1 | fsm + calculators | Java | low | low | Deferred |
| P2 | Remaining ~35 modules | Java | sparse | ~0% | Deferred (see list below) |

**Deferred services (not fully covered this run):** advertisement-service, asset-*, birth-death-services, bpa-calculator, challan-*, cnd-*, echallan-*, egov-user-event, estate-management, ewaste-services, garbage-service, inbox, land-services, ndc-*, noc-services, pet-services, pgr-ai-services, pqm*, property calculators, rainmaker-pgr, request-service, street-vending, tl-*, tp-services, turn-io-adapter, upyog-aggregation-service (Go), vehicle, vendor, ws-calculator, sw-calculator, and others under `municipal-services/` with no or minimal tests.

### Shared post-deploy smoke

Module: `municipal-services/core-smoke-tests` — REST Assured, `@Tag("smoke")`, `services.yaml`, Jenkinsfile, env-driven config.

**Smoke execution this run:** Not executed (no live `BASE_URL` / credentials in agent VM). `mvn test-compile` succeeds for smoke module.

**Java service builds this run:** Not executed — private Nexus (`upyog-nexus.niua.org`) unreachable from cloud agent; tests are structured for Jenkins CI.

**Node firenoc-services:** `npm run test:unit` **passed** locally.

---

## Phase 1–2 — implemented per service

### vendor-management (full batch)

- Unit: `VendorServiceTest`, `VendorUtilTest`, `MDMSValidatorTest`, `ResponseInfoFactoryTest`, `VendorControllerTest` (@WebMvcTest)
- ArchUnit: controllers must not depend on repositories
- Maven: Surefire/Failsafe split, JaCoCo baseline gates, OWASP profile
- README: `README-TESTING.md`

### pgr-services

- Unit: `ServiceRequestValidatorTest` (MDMS, search, plain search)
- Re-enabled `RequestsApiControllerTest` skeleton (@WebMvcTest only)
- Maven: Surefire/Failsafe, JaCoCo on `org.egov.pgr.validator`
- Smoke: `PgrServicesSmokeTest`

### property-services

- Unit: `PropertyValidatorCreateTest` (empty owners)
- Maven: Surefire/Failsafe, JaCoCo on validators
- Smoke: `PropertyServicesSmokeTest`

### community-hall-booking

- Existing 23 tests retained; Surefire excludes `*IT`, Failsafe runs ITs
- ArchUnit layering test
- JaCoCo verify gate on service/seatlock packages (baseline thresholds)

### firenoc-services (Node)

- Jest + babel-jest, `test:unit` / `test:integration` scripts
- Unit tests for `validateFireNOCSearchModel`

### core-smoke-tests

- Support: config, auth, tenant, polling, service registry
- Per-service smoke classes (property, pgr, ws, vendor, chb, firenoc)
- E2E: `PgrPropertyCrossServiceSmokeTest`
- Jenkins: parameterized pipeline; credential IDs documented

---

## Phase 3 — additional tests

| Type | Status |
|------|--------|
| Security / RBAC per endpoint | Partial via smoke auth cases; full matrix deferred |
| OWASP Dependency-Check | Profile `-Powasp` on vendor-management (replicate to others) |
| Migration tests with data | Deferred |
| Kafka schema / compatibility | Deferred |
| Pact / Spring Cloud Contract | Plan: property-services ↔ ws-services pilot; not implemented |
| Resilience (timeout/retry) | Deferred (CHB has seat lock ITs) |
| MDMS/workflow config validation | Deferred |
| ArchUnit | **Done** vendor-management, community-hall-booking |
| PIT / Stryker / k6 | Deferred (optional pipelines) |

---

## Phase 4 — conventions

Documented in `municipal-services/TESTING.md`: naming, Surefire vs Failsafe, smoke suite, coverage targets.

---

## Phase 6 — Jenkins

- `core-smoke-tests/Jenkinsfile` — shared smoke job
- `jenkins/deploy-with-smoke.groovy` — snippet for post-deploy trigger + pre-deploy test stages
- Scheduled ALL-services smoke: configure in Jenkins (30–60 min UAT/prod)

---

## Coverage before vs after (estimated)

| Service | Before (line, validator/service pkg) | After (with new tests) |
|---------|--------------------------------------|-------------------------|
| vendor-management | ~0% | ~15–25% (utils/service); gate floor 10% |
| pgr-services | ~0% | ~5–10% validator |
| property-services | ~0% | ~1–3% validator |
| community-hall-booking | moderate | unchanged + ArchUnit |
| firenoc-services | 0% (no Jest) | search validator covered |
| core-smoke-tests | n/a | black-box registry for 11 services |

Run `mvn test jacoco:report` in CI to refresh exact numbers.

---

## Findings (not fixed — production behaviour unchanged)

1. **pgr-services `ServiceRequestValidator.validateCreate`:** When `service` is null, code accumulates `SERVICE_MISSING` but then NPEs on `validateSource(request.getService().getSource())` before throwing — untestable as CustomException without fix.
2. **vendor-management `VendorService` constructor:** Duplicate parameters (`EnrichmentService`, `VendorRepository` twice) — confusing for DI/testing (documented; tests construct explicitly).
3. **vendor-management `MDMSValidator.validateMdmsData`:** No-op (TODO); MDMS validation not enforced on create.
4. **vendor-management `ResponseInfoFactory`:** Hard-coded `resMsgId` (`uief87324`).
5. **firenoc-services `package.json`:** Previously `test` ran only eslint — no unit coverage until this PR.
6. **Tenant isolation:** Many services rely on repository query filters; not all endpoints validated for cross-tenant leakage (smoke adds black-box check where read APIs exist).
7. **RequestsApiControllerTest:** Still minimal stubs — needs mocked `PGRService` for meaningful contract tests.

---

## Small refactors for testability

None in production code — tests use `@WebMvcTest`, Mockito, and explicit constructors only.

---

## How to verify locally

```bash
# Smoke (needs env)
cd municipal-services/core-smoke-tests && mvn -Psmoke -Dservice=property-services test

# Java service (needs Nexus)
cd municipal-services/vendor-management && mvn test && mvn verify

# Node
cd municipal-services/firenoc-services && npm run test:unit
```
