# Meaningful test expansion — progress summary

**Branch:** `cursor/municipal-services-test-strategy-572c` · **PR:** #1940 · **JDK:** 17 · **Settings:** `.mvn/settings-public.xml`

**Target (non-trivial code):** ≥70% line / ≥60% branch per module. **Stretch:** 80% / 70%.

## Step 1 complete

- Full-tree JaCoCo baseline captured (`testing/coverage_baseline.py --run --clean`).
- [`baseline.md`](baseline.md) — module line/branch %.
- [`baseline-per-class.json`](baseline-per-class.json) — per-class missed lines/branches.
- [`class-ranking.md`](class-ranking.md) — priority types (services, validators, builders, utils).
- [`shallow-tests.md`](shallow-tests.md) — **3938** scaffold / trivial tests flagged.

## Module progress

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meaningful tests added | Meets 70/60? |
|--------|----------------:|-----------:|------------------:|-------------:|-----------------------:|:------------:|
| property-services | 8.0 | 8.0* | 0.3 | 0.3* | 11 (`PropertyValidatorTest`, `PropertyQueryBuilderTest`; scaffolds removed) | No |
| pgr-services | 17.6 | — | 0.9 | — | 0 (already has `ServiceRequestValidatorTest`) | No |
| ws-services | 11.5 | — | 0.6 | — | 0 | No |
| sw-services | 11.3 | — | 0.5 | — | 0 | No |
| tl-services | 13.1 | — | 0.3 | — | 0 | No |
| bpa-services | 6.5 | — | 0.0 | — | 0 | No |
| *remaining 40 modules* | see baseline.md | — | — | — | 0 this run | No |

\*Property-services “new” % uses post-change `mvn test jacoco:report` with JaCoCo **excludes** for models/DTO/config; priority packages (validator + query builder) ~**6.9% / 6.4%** line/branch validator and **22% / 16%** builder.

## Next batches (ranked)

1. Remaining missed-line leaders from [`class-ranking.md`](class-ranking.md) in **property-services** (`EnrichmentService`, `PropertyService`, `AssessmentValidator`, Kafka consumer).
2. **ws-services**, **sw-services** calculators and validators.
3. **bpa-services**, **tl-services** workflow/fee services.

## Verify status

- **property-services:** `mvn clean test` PASS; `mvn verify` PASS with package-level JaCoCo gates (validator + builder floors).
- **Full 46-module `mvn clean verify`:** not re-run end-to-end this iteration (prior full `mvn test` green on JDK 17).
