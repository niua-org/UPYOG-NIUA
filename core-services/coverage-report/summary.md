# Coverage campaign summary

Branch: `feature/core-services-test-strategy` (fork PR [#1943](https://github.com/niua-org/UPYOG-NIUA/pull/1943)).

## Coverage table (non-POJO heuristic — `module_metrics.py`)

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meets 70/60? |
| --- | ---: | ---: | ---: | ---: | --- |
| upyog-draft-service | 95.2 | **95.2** | 83.3 | **83.3** | **Yes** |
| tenant | — | **89.9** | — | **53.6** | Line yes |
| gis-service | 49.6 | **76.9** | 36.9 | **63.9** | Line yes; branch ~64% |
| egov-workflow-v2 | 69.5 | **69.5** | 50.6 | **50.6** | Line ~yes; **branch &lt; 60%** |
| gateway | 32.2 | **51.4** | 32.5 | **47.2** | No |
| egov-idgen | 49.7 | **50.7** | 38.9 | **38.9** | No |
| mdms-v2 | 21.6 | **~27+** | 23.6 | **~28+** | No |
| egov-indexer | 1.5 | **16.2** | 0.9 | **15.5** | No (JaCoCo bundle **0.12**) |
| egov-user | 10.3 | **10.8** | 7.0 | **7.5** | No (JaCoCo domain.service **0.103**) |
| egov-pg-service | — | **~5.3** | — | **~1.1** | No |
| finance-collections-voucher-consumer | — | **~2.2** | — | **~0** | No |
| dashboard-analytics | — | **~0.9+** | — | **~0.6+** | No |
| pdf-service | ~99 | **~99** | ~96 | **~97** | **Yes** (Jest) |
| core-smoke-tests | N/A | N/A | N/A | N/A | RestAssured |
| egov-notification-sms | N/A | N/A | N/A | N/A | **Blocked** (SNAPSHOT deps) |

## `verify-all.sh` (latest — `coverage-report/verify-all.log`)

**SUMMARY: ok=14, fail=1**

| Module | Result |
| --- | --- |
| core-smoke-tests | OK |
| egov-idgen | OK |
| egov-indexer | OK |
| **egov-notification-sms** | **FAIL** (documented) |
| egov-pg-service | OK |
| egov-user | OK |
| egov-workflow-v2 | OK |
| gateway | OK |
| gis-service | OK |
| mdms-v2 | OK |
| tenant | OK |
| upyog-draft-service | OK |
| finance-collections-voucher-consumer | OK |
| dashboard-analytics | OK |
| pdf-service | OK |

## Shallow tests (15-module scope)

**0** entries in `shallow-tests.md` (fixed `UserRequestControllerTest`, `BusinessServiceControllerTest`, `EscalationControllerTest`).

## Commits (this pass)

| Commit | Module |
| --- | --- |
| `b418b7e2e7` | egov-indexer — listener + DataTransformation; JaCoCo **0.12** |
| `33c27c0ae9` | egov-user — UserService/TokenService; controller test; JaCoCo **0.103** |
| `334585c869` | mdms-v2 — ThemeConfigServiceImpl |
| `97b2abe8d0` | gateway — CorrIdFormDataFilterHelper |
| `334a13dab1` | egov-workflow-v2 — controller verify; service branch gate **0.50** |
| `01fb3827d2` | egov-pg-service — TransactionService verify |
| `9835d7b9ab` | finance consumer — InstrumentStatusEnum |
| `94a50fd7cc` | dashboard-analytics — ComputeHelperFactory |

(Prior commits on branch include gateway RbacPreCheck, mdms ResponseUtil, gis AssetServiceAdapter, etc.)

## Still left

- **70/60**: gateway remaining helpers (PreHook, AuthPreCheck form data), mdms v2 WorkflowServiceImpl / MDMSServiceV2, egov-user depth (password/OAuth paths), indexer Legacy/Reindex consumers, finance VoucherServiceImpl, dashboard QueryServiceImpl/handlers, pg payments/gateway services.
- **Workflow branch 60%**: module still **50.6%** — needs EnrichmentService / TransitionService branch batches.
- **egov-notification-sms**: environment only.
- **Production bug**: gateway `ExceptionUtils` 4xx→500 (`bugs-found.md`).

`baseline.md` regenerated via `generate-baseline.sh` on this pass.
