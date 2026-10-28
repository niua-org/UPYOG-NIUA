# Meaningful test expansion — progress summary

**Branch:** `cursor/municipal-services-test-strategy-572c` · **PR:** #1940 · **JDK:** 17

**Target (non-trivial code, JaCoCo excludes models/DTO/config where configured):** ≥70% line / ≥60% branch.

## Module progress

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meaningful tests added | 70/60 met? |
|--------|----------------:|-----------:|------------------:|-------------:|-----------------------:|:----------:|
| property-services | 8.0 (audit) / 2.2 (class baseline) | **4.6** | 0.3 / 1.5 | **4.6** | ~35 methods across validator, builder, util, service, consumer | No — large services still untested |
| ws-services | 11.5 (audit) | **1.8** | 0.6 | **1.1** | 4 (`ActionValidatorTest`, `ResponseInfoFactoryTest`) | No |
| sw-services | 11.3 | **9.7** | 0.5 | **9.0** | 38 methods (`ActionValidatorTest`, `SewerageConnectionValidatorTest`, `SWQueryBuilderTest`, util/validator/service batch) | No |
| pgr-services | 17.6 | — | 0.9 | — | 0 (existing validator tests) | No |
| tl-services | 13.1 | — | 0.3 | — | 0 | No |
| bpa-services | 6.5 | — | 0.0 | — | 0 | No |
| *remaining 40 modules* | see [baseline.md](baseline.md) | — | — | — | 0 | No |

## Tooling

- [`testing/coverage_baseline.py`](../testing/coverage_baseline.py) — baseline + shallow detection
- [`testing/purge_generated_scaffolds.py`](../testing/purge_generated_scaffolds.py) — remove generated scaffolds in priority packages
- [`testing/record_module_coverage.py`](../testing/record_module_coverage.py) — `LINE=` / `BRANCH=` after jacoco

## Verify

| Module | `mvn clean verify` |
|--------|-------------------|
| property-services | PASS (package JaCoCo gates) |
| ws-services | PASS (util/validator floors 1% / 3% branch) |
| All 46 | Pending end-of-run sweep |
