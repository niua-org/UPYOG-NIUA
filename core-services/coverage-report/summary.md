# Coverage campaign summary

Branch: `feature/core-services-test-strategy` (fork PR [#1943](https://github.com/niua-org/UPYOG-NIUA/pull/1943)).

## Module list (15)

| # | Module | Path |
|---|--------|------|
| 1 | core-smoke-tests | `core-services/core-smoke-tests` |
| 2 | egov-idgen | `core-services/egov-idgen` |
| 3 | egov-indexer | `core-services/egov-indexer` |
| 4 | egov-notification-sms | `core-services/egov-notification-sms` |
| 5 | egov-pg-service | `core-services/egov-pg-service` |
| 6 | egov-user | `core-services/egov-user` |
| 7 | egov-workflow-v2 | `core-services/egov-workflow-v2` |
| 8 | gateway | `core-services/gateway` |
| 9 | gis-service | `core-services/gis-service` |
| 10 | mdms-v2 | `core-services/mdms-v2` |
| 11 | tenant | `core-services/tenant` |
| 12 | upyog-draft-service | `core-services/upyog-draft-service` |
| 13 | pdf-service | `core-services/pdf-service` (Jest) |
| 14 | finance-collections-voucher-consumer | `business-services/finance-collections-voucher-consumer` |
| 15 | dashboard-analytics | `business-services/dashboard-analytics` |

## Coverage table (non-POJO heuristic — `module_metrics.py` / `parse_jacoco.py`)

| Module | Baseline line % | New line % | Baseline branch % | New branch % | Meets 70/60? |
| --- | ---: | ---: | ---: | ---: | --- |
| upyog-draft-service | 95.2 | **95.2** | 83.3 | **83.3** | **Yes** |
| tenant | — | **89.9** | — | **53.6** | Line yes, branch no |
| gis-service | 49.6 | **76.9** | 36.9 | **63.9** | Line yes; branch ~yes (63.9%) |
| egov-workflow-v2 | 69.5 | **69.5** | 50.6 | **50.6** | Line ~yes, branch no |
| gateway | 32.2 | **51.4** | 32.5 | **47.2** | No |
| egov-idgen | 49.7 | **50.7** | 38.9 | **38.9** | No |
| mdms-v2 | 21.6 | **26.6** | 23.6 | **27.9** | No |
| egov-indexer | 1.5 | **12.0** | 0.9 | **13.0** | No (custom POJOs excluded in gate) |
| egov-user | 10.3 | **10.2** | 7.0 | **7.1** | No |
| egov-pg-service | — | **5.3** | — | **1.1** | No |
| finance-collections-voucher-consumer | — | **2.2** | — | **0.0** | No |
| dashboard-analytics | — | **0.9** | — | **0.6** | No |
| core-smoke-tests | N/A | N/A | N/A | N/A | RestAssured (no JaCoCo bundle) |
| pdf-service | ~99 | **~99** | ~96 | **~97** | **Yes** (Jest) |
| egov-notification-sms | N/A | N/A | N/A | N/A | **Blocked** (SNAPSHOT deps) |

## JaCoCo `check` on modules

All Maven modules in scope except **egov-notification-sms** and **core-smoke-tests** run JaCoCo verify at **measured bundle floors** (updated this pass for indexer, gateway, mdms-v2, gis-service, idgen, pg-service, dashboard-analytics, finance consumer, tenant, upyog-draft, egov-user, workflow-v2).

## `verify-all.sh` (latest — `coverage-report/verify-all.log`)

**SUMMARY: ok=14, fail=1**

| Module | Result |
| --- | --- |
| core-smoke-tests | OK |
| egov-idgen | OK |
| egov-indexer | OK |
| egov-notification-sms | **FAIL** (tracer SNAPSHOT / Nexus — documented in `untestable.md`) |
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

Regenerated: **3** remaining (`shallow-tests.md`):

- `egov-user` — `UserRequestControllerTest` (several tests missing `@Test` / assertions)
- `egov-workflow-v2` — `BusinessServiceControllerTest`, `EscalationControllerTest`

Removed or fixed this pass: idgen controller, pg ignored integration tests, finance empty context test, tenant create verify, pg WebMvc verify.

## Commits (this continuation)

| Commit | Module / area |
| --- | --- |
| `178d1543bd` | egov-indexer — IndexerUtils, ResponseInfoFactory |
| `c79f4b0460` | gateway — RbacPreCheckFilterHelper + JaCoCo |
| `3dd91ccbd7` | mdms-v2 — ResponseUtil, composite ID util |
| `2a40a31e66` | egov-idgen — controller test + JaCoCo |
| `2a3ba16cfe` | gis-service — AssetServiceAdapter expansion |
| `edbc60edac` | tenant — controller verify |
| `2275bd0f73` | finance consumer — PaymentModeEnum; drop empty context test |
| `354c1d7ec0` | dashboard-analytics — JSONObjectUtil |
| `8b37ccdec9` | egov-pg-service — drop ignored repo tests; validator verify |
| `39decde79a` | egov-pg-service — controller verify |
| `6bdc229e04` | egov-indexer — JaCoCo floor tune |

(Prior commits on branch: `8443caa2c0` dashboard factory, `91665d6727` finance query builder, `8d6d2b1188` gis, `0d5d5f85b6` user criteria, etc.)

## Still left

- **70/60 targets**: gateway (remaining filter helpers / utils), mdms-v2 (v2 services/repos), egov-user (services), egov-indexer (IndexerService, consumers, Legacy/Reindex), finance consumer (services), dashboard-analytics (handlers/query), egov-pg-service (services beyond WebMvc slice).
- **Shallow tests**: 3 controller tests listed above.
- **egov-notification-sms**: environment/Nexus only.
- **Production bug** (not fixed): gateway `ExceptionUtils` 4xx→500 — `bugs-found.md`.

`baseline.md` regenerated at **2026-10-28T07:54:58Z** via `generate-baseline.sh`.
