# Meaningful test expansion — progress summary

**Branch:** `cursor/municipal-services-test-strategy-572c` · **PR:** #1940 · **JDK:** 17

**Target (non-trivial code, JaCoCo excludes models/DTO/config where configured):** ≥70% line / ≥60% branch.

## Module progress

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meaningful tests added | 70/60 met? |
|--------|----------------:|-----------:|------------------:|-------------:|-----------------------:|:----------:|
| property-services | 8.0 (audit) / 2.2 (class baseline) | **4.6** | 0.3 / 1.5 | **4.6** | ~35 methods across validator, builder, util, service, consumer | No |
| ws-services | 11.5 (audit) | **1.8** | 0.6 | **1.1** | 4 (`ActionValidatorTest`, `ResponseInfoFactoryTest`) | No |
| sw-services | 11.3 | **9.7** | 0.5 | **9.0** | 38 methods (validator, util, builder batch) | No |
| pgr-services | 17.6 | **13.4** | 0.9 | **13.4** | ~24 methods (validator, builder, util, service) | No |
| tl-services | 13.1 | **11.2** | 0.3 | **9.3** | ~37 methods (validator, util, builder, workflow) | No |
| bpa-services | 6.5 | **9.3** | 0.0 | **8.4** | 27 methods (validator, util, workflow, builder) | No |
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
| fsm | 0.0 | **8.3** | 0.0 | **3.2** | ~12 methods (util, workflow, querybuilder, validator; purged 25 scaffolds) | No |
| street-vending | 0.0 | **8.6** | 0.0 | **6.2** | ~8 methods (util, validator, querybuilder; purged 28 scaffolds) | No |
| *remaining 25 modules* | see [baseline.md](baseline.md) | — | — | — | 0 | No |

## Tooling

- [`testing/coverage_baseline.py`](../testing/coverage_baseline.py) — baseline + shallow detection
- [`testing/purge_generated_scaffolds.py`](../testing/purge_generated_scaffolds.py) — remove generated scaffolds in priority packages
- [`testing/record_module_coverage.py`](../testing/record_module_coverage.py) — `LINE=` / `BRANCH=` after jacoco
- [`testing/set_util_jacoco_floor.py`](../testing/set_util_jacoco_floor.py) — align util/utils/validation package JaCoCo floors after meaningful tests

## Verify

| Module | `mvn clean verify` |
|--------|-------------------|
| property-services | PASS (package JaCoCo gates) |
| ws-services | PASS (util/validator floors) |
| sw-services | PASS (validator/util/builder package JaCoCo gates) |
| pgr-services | PASS (validator/util/service/rowmapper package JaCoCo gates) |
| bpa-services | PASS (validator/util/workflow/querybuilder package JaCoCo gates) |
| tl-services | PASS (validator/util/builder/workflow package JaCoCo gates) |
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
| fsm | PASS (validator/util/workflow/querybuilder package JaCoCo gates) |
| street-vending | PASS (validator/util/querybuilder package JaCoCo gates) |
| All 46 | Pending end-of-run sweep |
