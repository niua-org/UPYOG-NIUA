# Meaningful test expansion — progress summary

**Branch:** `cursor/municipal-services-test-strategy-572c` · **PR:** #1940 · **JDK:** 17

**Target (non-trivial code, JaCoCo excludes models/DTO/config where configured):** ≥70% line / ≥60% branch.

## Module progress

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meaningful tests added | 70/60 met? |
|--------|----------------:|-----------:|------------------:|-------------:|-----------------------:|:----------:|
| property-services | 8.0 (audit) / 2.2 (class baseline) | **4.6** → **12.8** | 0.3 / 1.5 | **4.6** → **9.7** | Deep pass (round 2): notification/workflow/consumer tests + extended `PropertyValidatorTest`; JaCoCo gates on **service**, **util**, **consumer**, validator, builder | No |
| ws-services | 11.5 (audit) | **1.8** → **10.7** | 0.6 | **1.1** → **7.0** | Deep pass: `WaterServiceImplTest`, `EnrichmentServiceTest`, `EditNotificationServiceTest`, `WaterConnectionValidatorTest`, workflow/consumer tests; JaCoCo gates on **service**, **util**, **validator** | No |
| sw-services | 11.3 | **9.7** → **14.8** | 0.5 | **9.0** → **12.8** | Deep pass: `SewerageServiceImplTest`, `EnrichmentServiceTest`; JaCoCo gates on **service**, **util**, **validator**, **builder** | No |
| pgr-services | 17.6 | **13.4** → **31.3** | 0.9 | **13.4** → **22.8** | Deep pass: `PGRServiceTest`, `WorkflowServiceTest`, `UserServiceTest`, extended `EnrichmentServiceTest`, consumer + validator tests; raised **service** package JaCoCo floors | No |
| tl-services | 13.1 | **11.2** → **15.3** | 0.3 | **9.3** → **13.6** | Deep pass: `TradeLicenseServiceTest`, `EnrichmentServiceTest`, `TradeLicenseConsumerTest`, extended `TLValidatorTest`; JaCoCo gates on **service**, **consumer**, validator, util, builder, workflow | No |
| bpa-services | 6.5 | **9.3** → **11.6** | 0.0 | **8.4** → **9.3** | Deep pass: `BPAServiceTest`, `EnrichmentServiceTest`, `BPAConsumerTest`; JaCoCo gates on **service**, **consumer**, validator, util, workflow, querybuilder | No |
| ws-calculator | 0.0 | **11.3** | 0.0 | **1.1** | ~12 methods (`CalculatorUtilTest`, `MeterReadingUtilTest`, `WSCalculatorQueryBuilderTest`; purged 21 service/util/validator scaffolds) | No |
| sw-calculator | 0.0 | **10.7** | 0.0 | **0.6** | ~4 methods (`CalculatorUtilsTest`, `SWCalculatorQueryBuilderTest`; purged 15 scaffolds) | No |
| pt-calculator-v2 | 0.0 | **13.3** | 0.0 | **0.6** | ~3 methods (`BillingSlabQueryBuilderTest`, `CalculatorUtilsTest`; purged 19 scaffolds) | No |
| cnd-calculator | 0.0 | **30.7** | 0.0 | **1.4** | ~4 methods (`CalculationUtilsTest`; purged 7 kafka/service/util scaffolds) | No |
| echallan-calculator | 0.0 | **17.4** | 0.0 | **0.5** | ~3 methods (`CalculationUtilsTest`; purged 2 service scaffolds) | No |
| fsm-calculator | 0.0 | **23.7** | 0.0 | **1.5** | ~6 methods (`BillingSlabQueryBuilderTest`, `CalculationUtilsTest`; purged 3 scaffolds) | No |
| tl-calculator | 0.0 | **19.0** | 0.0 | **0.8** | ~3 methods (`CalculationQueryBuilderTest`, `CalculationUtilsTest`; purged 11 scaffolds) | No |
| bpa-calculator | 0.0 | **14.7** | 0.0 | **1.9** | ~4 methods (`PreapprovedPlanQueryBuilderTest`, `CalculationUtilsTest`; purged 1 scaffold) | No |
| ndc-calculator | 0.0 | **19.4** | 0.0 | **1.0** | ~6 methods (`CalculatorUtilsTest`, `ResponseInfoFactoryTest`, existing `SanitizeHtmlValidatorTest`) | No |
| challan-generation-calculator | 0.0 | **11.8** | 0.0 | **0.4** | ~2 methods (`CalculationUtilsTest`; purged 2 service scaffolds) | No |
| inbox | 0.0 | **3.9** | 0.0 | **1.8** | ~14 methods (util, `WorkflowServiceTest`, `InboxQueryBuilderTest`, `ValidatorDefaultImplementationTest`; purged 60 scaffolds) | No |
| community-hall-booking | 0.0 | **26.1** | 0.0 | **22.1** | ~13 methods (validator, util, querybuilder; purged 25 scaffolds) | No |
| garbage-service | 0.0 | **6.3** | 0.0 | **5.6** | ~6 methods (util, `GarbageAccountQueryBuilderTest`; purged 15 scaffolds) | No |
| fsm | 0.0 | **8.3** → **9.4** | 0.0 | **3.2** → **7.0** | Deep pass: `FSMServiceTest`, `EnrichmentServiceTest`, `FSMQueryBuilderTest`, `FSMValidatorTest`, `ActionValidatorTest`, `FSMUtilTest`; JaCoCo **service** package gate | No |
| street-vending | 0.0 | **8.6** | 0.0 | **6.2** | ~8 methods (util, validator, querybuilder; purged 28 scaffolds) | No |
| ndc-services | 0.0 | **15.0** | 0.0 | **9.9** | ~15 methods (validator, util, builder, config; purged 14 scaffolds) | No |
| request-service | 0.0 | **5.3** | 0.0 | **6.5** | ~12 methods (util, querybuilder, validation; purged 24 scaffolds) | No |
| pet-services | 0.0 | **7.8** | 0.0 | **5.6** | ~8 methods (util, validator; purged 19 scaffolds) | No |
| rainmaker-pgr | 0.0 | **6.3** | 0.0 | **2.2** | ~10 methods (utils, repository; purged 6 scaffolds) | No |
| noc-services | 0.0 | **5.1** | 0.0 | **2.5** | ~6 methods (validator, builder; purged 15 scaffolds) | No |
| vendor | 0.0 | **6.2** | 0.0 | **2.9** | ~10 methods (validator, util, querybuilder; purged 14 scaffolds) | No |
| advertisement-service | 0.0 | **16.7** | 0.0 | **3.0** | ~18 methods (validator, util, querybuilder; purged service/kafka scaffolds) | No |
| asset-calculator | 0.0 | **24.5** | 0.0 | **4.3** | ~8 methods (`CalculationUtilsTest`, `MDMSValidatorTest`, `ResponseInfoFactoryTest`) | No |
| asset-services | 0.0 | **22.5** | 0.0 | **1.7** | ~6 methods (`AssetUtilTest`, `AssetQueryBuilderTest`, `ResponseInfoFactoryTest`) | No |
| birth-death-services | 0.0 | **7.3** | 0.0 | **0.5** | ~6 methods (`CommonUtilsTest`, `ResponseInfoFactoryTest`; purged service scaffolds) | No |
| challan-generation | 0.0 | **11.5** | 0.0 | **0.5** | ~5 methods (`SanitizeHtmlValidatorTest`, `ResponseInfoFactoryTest`; purged service scaffolds) | No |
| cnd-service | 0.0 | **12.2** | 0.0 | **0.2** | ~4 methods (`SanitizeHtmlValidatorTest`; purged service/kafka scaffolds) | No |
| echallan-services | 0.0 | **14.1** | 0.0 | **0.7** | ~4 methods (`CommonUtilsTest`, `ResponseInfoFactoryTest`) | No |
| egov-user-event | 0.0 | **11.3** | 0.0 | **1.4** | ~6 methods (`UserEventsUtilsTest`, `ResponseInfoFactoryTest`) | No |
| estate-management | 0.0 | **9.2** | 0.0 | **0.5** | ~8 methods (`BillingPeriodUtilTest`, `EstateUtilTest`, `ResponseInfoFactoryTest`) | No |
| ewaste-services | 0.0 | **13.1** | 0.0 | **1.2** | ~4 methods (`EwasteUtilTest`, `ResponseInfoFactoryTest`) | No |
| land-services | 0.0 | **11.9** | 0.0 | **1.3** | ~4 methods (`LandMDMSValidatorTest`, `ResponseInfoFactoryTest`) | No |
| pgr-ai-services | 0.0 | **12.8** | 0.0 | **0.4** | ~4 methods (`SanitizeHtmlValidatorTest`, `ResponseInfoFactoryTest`) | No |
| pqm | 0.0 | **12.8** | 0.0 | **0.5** | ~6 methods (`JsonParserTest`, `ResponseInfoFactoryTest`) | No |
| pqm-anomaly-finder | 0.0 | **20.3** | 0.0 | **1.0** | ~6 methods (`PqmAnomalyFinderUtilTest`, `ResponseInfoFactoryTest`) | No |
| pt-services-v2 | 0.0 | **11.8** | 0.0 | **0.4** | ~4 methods (`PropertyUtilTest`, `ResponseInfoFactoryTest`) | No |
| tp-services | 0.0 | **19.0** | 0.0 | **1.3** | ~4 methods (`SanitizeHtmlValidatorTest`, `ResponseInfoFactoryTest`) | No |
| turn-io-adapter | 0.0 | **14.7** | 0.0 | **0.2** | ~8 methods (`AdditionalDetailValidatorTest`, `MDMSUtilsTest`) | No |
| vehicle | 0.0 | **9.1** | 0.0 | **0.6** | ~4 methods (`SanitizeHtmlValidatorTest`, `ResponseInfoFactoryTest`) | No |
| vendor-management | 0.0 | **32.2** | 0.0 | **1.5** | ~6 methods (`VendorUtilTest`, `EnrichmentServiceTest`, `ResponseInfoFactoryTest`) | No |
| firenoc-services | — | — | — | — | Jest behavioral `firenocCalculatorService` (mocked `httpRequest`) | N/A (Node) |
| firenoc-calculator | — | — | — | — | Jest behavioral `firenocService.getFireNoc` | N/A (Node) |
| pqm-scheduler | — | — | — | — | pytest mocks for `getUser`, `triggerCronjob` | N/A (Python) |

## Tooling

- [`testing/coverage_baseline.py`](../testing/coverage_baseline.py) — baseline + shallow detection
- [`testing/purge_generated_scaffolds.py`](../testing/purge_generated_scaffolds.py) — remove generated scaffolds in priority packages
- [`testing/record_module_coverage.py`](../testing/record_module_coverage.py) — `LINE=` / `BRANCH=` after jacoco
- [`testing/set_util_jacoco_floor.py`](../testing/set_util_jacoco_floor.py) — align util/utils/validation package JaCoCo floors after meaningful tests
- [`testing/update_gate_floor.py`](../testing/update_gate_floor.py) — align module pom gate package floors from JaCoCo CSV
- [`testing/verify_module.sh`](../testing/verify_module.sh) — `test` → `jacoco:report` → floor update → `verify`

## Verify

| Module | `mvn clean verify` |
|--------|-------------------|
| property-services | PASS (validator + builder + **service** package JaCoCo gates, deep-pass floors) |
| ws-services | PASS (service/util/validator package JaCoCo gates) |
| sw-services | PASS (service/util/validator/builder package JaCoCo gates) |
| pgr-services | PASS (validator/util/service/rowmapper package JaCoCo gates) |
| bpa-services | PASS (validator/util/workflow/querybuilder/**service**/**consumer** package JaCoCo gates, deep-pass floors) |
| tl-services | PASS (validator/util/builder/workflow/**service**/**consumer** package JaCoCo gates, deep-pass floors) |
| ws-calculator | PASS (util package JaCoCo gate) |
| sw-calculator | PASS (util package JaCoCo gate) |
| pt-calculator-v2 | PASS (util package JaCoCo gate) |
| cnd-calculator | PASS (util package JaCoCo gate) |
| echallan-calculator | PASS (util package JaCoCo gate) |
| fsm-calculator | PASS (utils package JaCoCo gate) |
| tl-calculator | PASS (utils package JaCoCo gate) |
| bpa-calculator | PASS (utils package JaCoCo gate) |
| ndc-calculator | PASS (validation package JaCoCo gate) |
| challan-generation-calculator | PASS (util package JaCoCo gate) |
| inbox | PASS (util/builder/V2.validator package JaCoCo gates) |
| community-hall-booking | PASS (validator/util/querybuilder package JaCoCo gates) |
| garbage-service | PASS (util/builder package JaCoCo gates) |
| fsm | PASS (validator/util/workflow/querybuilder/**service** package JaCoCo gates, deep-pass floors) |
| street-vending | PASS (validator/util/querybuilder package JaCoCo gates) |
| ndc-services | PASS (validator/util/builder package JaCoCo gates) |
| request-service | PASS (validation/util/querybuilder package JaCoCo gates) |
| pet-services | PASS (validator/util package JaCoCo gates) |
| rainmaker-pgr | PASS (utils/repository gates; `verify` with `-Dspring-boot.repackage.skip=true` on Boot 2.2 parent) |
| noc-services | PASS (validator/builder package JaCoCo gates) |
| vendor | PASS (validator/util/querybuilder package JaCoCo gates) |
| advertisement-service | PASS (validator package JaCoCo gate) |
| asset-calculator | PASS (validator package JaCoCo gate) |
| asset-services | PASS (util package JaCoCo gate) |
| birth-death-services | PASS (utils gate; `-Dspring-boot.repackage.skip=true`) |
| challan-generation | PASS (validation package JaCoCo gate) |
| cnd-service | PASS (validation package JaCoCo gate) |
| echallan-services | PASS (util package JaCoCo gate) |
| egov-user-event | PASS (utils package JaCoCo gate) |
| estate-management | PASS (upyog.util package JaCoCo gate) |
| ewaste-services | PASS (util package JaCoCo gate) |
| land-services | PASS (validator package JaCoCo gate) |
| pgr-ai-services | PASS (validation package JaCoCo gate) |
| pqm | PASS (util gate; `-Dspring-boot.repackage.skip=true`) |
| pqm-anomaly-finder | PASS (util gate; `-Dspring-boot.repackage.skip=true`) |
| pt-services-v2 | PASS (util package JaCoCo gate) |
| tp-services | PASS (validation package JaCoCo gate) |
| turn-io-adapter | PASS (v2.models package JaCoCo gate) |
| vehicle | PASS (validation package JaCoCo gate) |
| vendor-management | PASS (utils/service package JaCoCo gates) |
| firenoc-services | PASS (`npm run test:unit`) |
| firenoc-calculator | PASS (`npm run test:unit`) |
| pqm-scheduler | PASS (`pytest tests/`) |
| All 45 Java service modules | **Final sweep (2026-10-28):** `testing/verify_all_modules.sh` → `/tmp/verify-all-final.log` — **45/45 PASS** after aligning `fsm` multi-package JaCoCo floors (`core-smoke-tests` excluded: post-deploy HTTP smokes need live `BASE_URL`) |
| fsm | PASS (validator/util/workflow/querybuilder floors synced to measured JaCoCo) |
| core-smoke-tests | **Not in sweep** — requires deployed stack; POM fixed (removed orphan `spring-boot-starter-test` without version) |

## 70% line / 60% branch — outcome

**No Java module meets both thresholds** on measured module JaCoCo totals (with per-module excludes for models/DTO/config where configured).

| Closest modules (line / branch) | Why 70/60 not reached |
|--------------------------------|------------------------|
| vendor-management **32.2% / 1.5%** | Branch coverage dominated by service/workflow paths; only util/service package gates raised |
| cnd-calculator **30.7% / 1.4%** | Calculator utils covered; billing/kafka services purged scaffolds without deep replacements |
| community-hall-booking **26.1% / 22.1%** | Best branch % in tree; still missing controller/service/integration depth |
| asset-calculator **24.5% / 4.3%** | MDMS/validation helpers tested; demand/billing services largely untouched |
| fsm-calculator **23.7% / 1.5%** | Query builder + utils only |

**Common blockers:** (1) multi-thousand-line `*Service` types needing repository + workflow + MDMS mocks; (2) Kafka consumers and schedulers; (3) commented/dead code (`ReceiptService` in pt-calculator-v2); (4) production quirks documented in [`untestable.md`](untestable.md) / [`bugs-found.md`](bugs-found.md). Next depth pass should follow [`class-ranking.md`](class-ranking.md) top missed lines per module (property-services `MigrationService` / `PropertyService` first).

## Work completed this epic

- Purged generated scaffolds in priority packages across **all** Java service modules; replaced with meaningful JUnit 5 + Mockito tests on validators, query builders, utils, and selected services.
- Per-module JaCoCo **package verify floors** aligned via [`testing/update_gate_floor.py`](../testing/update_gate_floor.py) / [`verify_module.sh`](../testing/verify_module.sh).
- **Node:** firenoc-services, firenoc-calculator (Jest, mocked HTTP). **Python:** pqm-scheduler (pytest, mocked `requests`).
