# Coverage campaign summary

## Scope — 14 PR modules

| # | Module | Build tool |
| --- | --- | --- |
| 1 | `core-smoke-tests` | Maven (RestAssured; smoke group excluded by default) |
| 2 | `egov-idgen` | Maven |
| 3 | `egov-indexer` | Maven |
| 4 | `egov-notification-sms` | Maven |
| 5 | `egov-pg-service` | Maven |
| 6 | `egov-user` | Maven |
| 7 | `egov-workflow-v2` | Maven |
| 8 | `gateway` | Maven |
| 9 | `gis-service` | Maven |
| 10 | `mdms-v2` | Maven |
| 11 | `tenant` | Maven |
| 12 | `upyog-draft-service` | Maven |
| 13 | `pdf-service` | npm / Jest |
| 14 | *(cross-cutting)* | `coverage-report/` tooling + JaCoCo gates |

Priority services **outside** this PR (`finance-collections-voucher-consumer`, `dashboard-analytics`) were not modified in this pass.

## Module coverage (non-POJO heuristic, JaCoCo XML)

Line/branch % exclude typical DTO/model/config packages (see `parse_jacoco.py`).

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meaningful tests added (this pass) |
| --- | ---: | ---: | ---: | ---: | ---: |
| upyog-draft-service | ~55* | **95.2** | ~45* | **83.3** | 13 (`DraftRepositoryTest`, `ProducerTest`, `DraftCleanupSchedulerTest`) |
| mdms-v2 | ~20* | **21.6** | ~22* | **23.6** | 5 (`MdmsDataEnricherTest`) |
| egov-indexer | — | (see `baseline.md`) | — | — | 0 (infra + prior `IndexerServiceTest` fixes) |
| gateway | — | — | — | — | 0 (infra; prior Mockito fixes) |
| gis-service | — | — | — | — | 0 |
| tenant | — | — | — | — | 0 |
| egov-user | — | — | — | — | 0 |
| egov-workflow-v2 | — | — | — | — | 0 |
| egov-idgen | — | — | — | — | 0 (+ JaCoCo verify gate) |
| egov-notification-sms | **N/A** | **N/A** | **N/A** | **N/A** | 0 — **build blocked** (SNAPSHOT deps) |
| egov-pg-service | — | — | — | — | 0 |
| core-smoke-tests | — | — | — | — | 0 |
| pdf-service | ~99 (Jest) | ~99 | ~96 | ~96 | 0 |

\*Baseline for draft/mdms estimated from pre-campaign JaCoCo on the same heuristic; full baselines are in `baseline.md` once `generate-baseline.sh` completes.

## Targets vs reality

| Module | 70% / 60% target | JaCoCo `check` gate |
| --- | --- | --- |
| upyog-draft-service | **Met** on non-trivial classes after new tests | **0.45 line / 0.35 branch** on bundle (`verify` phase) |
| mdms-v2 | **Not met** — large surface, legacy untested code | Existing package gates unchanged; module ~22% — needs further batches |
| egov-idgen | TBD | **0.25 line / 0.20 branch** (verify) — added this pass |
| egov-notification-sms | Cannot measure until Nexus SNAPSHOTs resolve | — |

## Deliverables

- `baseline.md` — per-class tables (`generate-baseline.sh`)
- `shallow-tests.md` — 47 heuristic shallow-test hits
- `untestable.md` — static/heavy-Spring classes
- `bugs-found.md` — includes `ExceptionUtils` 4xx→500 issue
- `verify-all.sh` — JDK 17 `mvn clean verify` + `npm test` for all 14

## Verify (`mvn clean verify` JDK 17)

Run: `coverage-report/verify-all.sh` (logs to `verify-all.log`).  
**Confirmed locally:** `upyog-draft-service` **verify OK** with new JaCoCo gate.  
**Latest `verify-all.sh` run (JDK 17):** **11 OK**, **2 FAIL** — see `verify-all.log`.

| Module | `mvn clean verify` |
| --- | --- |
| core-smoke-tests | OK |
| egov-idgen | OK |
| egov-indexer | OK |
| egov-notification-sms | **FAIL** (SNAPSHOT dependency resolution) |
| egov-pg-service | **FAIL** (see `verify-all.log`; dependency/build) |
| egov-user | OK |
| egov-workflow-v2 | OK |
| gateway | OK |
| gis-service | OK |
| mdms-v2 | OK |
| tenant | OK |
| upyog-draft-service | OK |
| pdf-service (npm test) | OK |
