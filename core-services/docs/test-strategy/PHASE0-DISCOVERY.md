# Phase 0 discovery

Coverage baseline: **not measured**.

Smoke tests were **not** executed against a live environment. Paths below come from one inventory of directories that contain `pom.xml` or `package.json`, excluding `build/`, `docs/`, `libraries/`, and `core-smoke-tests/`. Context path and health/management lines were read only from `application.properties`, `application.yml`, and `application.yaml` (test resources excluded).

Ordering is `git log` change count for the top-level folder under `core-services`, highest first. Nested modules share that parent count, then sort by name.

The workspace root matched as a node package and was excluded. Its walk mixed every child config into one row, so it is not a service.

`management.endpoints.web.base-path=/` is recorded as a management base path. It does not name the health endpoint, so `healthPath` stays empty and the smoke test skips health. `zuul.health.enabled=true` is a flag, not a URL. `egov-filestore` mentions `/health` and `/info` only inside `tracer.filterSkipPattern`; those tokens were not copied into `healthPath` or `versionPath`.

The only literal health URL in application config is gateway `otel.instrumentation.http.server.ignore-urls=/gateway/health,/gateway/promethus`. Catalog `healthPath` for `gateway` is `/gateway/health`. No `versionPath` was set. Read, write, auth, and tenant paths are empty on every row.

`national-dashboard-ingest` and `national-dashboard-kafka-pipeline` both set context path `/national-dashboard`.

| Service | Kind | Context path | Health / management in config | Git changes |
| --- | --- | --- | --- | --- |
| egov-user | maven | `/user` | none | 1504 |
| egov-pg-service | maven | `/pg-service` | `management.endpoints.web.base-path=/` | 820 |
| egov-workflow-v2 | maven | `/egov-workflow-v2` | `management.endpoints.web.base-path=/` | 422 |
| national-dashboard-ingest | maven | `/national-dashboard` | none | 403 |
| egov-indexer | maven | `/egov-indexer` | none | 394 |
| egov-common-masters | maven | `/egov-common-masters` | none | 380 |
| egov-location | maven | `/egov-location` | `management.endpoints.web.base-path=/` | 354 |
| pdf-service | node | none | none | 276 |
| egov-accesscontrol | maven | `/access` | `management.endpoints.web.base-path=/` | 269 |
| egov-filestore | maven | `/filestore` | `management.endpoints.web.base-path=/`; tracer skip pattern also lists `/health` and `/info` | 266 |
| report | maven | `/report` | `management.endpoints.web.base-path=/` | 235 |
| audit-service | maven | `/audit-service` | none | 226 |
| service-request | maven | `/service-request` | none | 220 |
| mdms-v2 | maven | `/mdms-v2` | `management.endpoints.web.base-path=/` | 217 |
| egov-data-uploader | maven | `/data-uploader` | none | 210 |
| egov-persister | maven | `/common-persist` | none | 207 |
| egov-notification-sms | maven | `/notification-sms` | none | 202 |
| egov-idgen | maven | `/egov-idgen` | `management.endpoints.web.base-path=/` | 190 |
| chatbot | maven | `/chatbot` | none | 188 |
| egov-survey-services | maven | `/egov-survey-services` | none | 188 |
| egov-localization | maven | `/localization` | none | 181 |
| egov-otp | maven | `/otp` | `management.endpoints.web.base-path=/` | 179 |
| zuul | maven | none | `zuul.health.enabled=true`; `management.endpoints.web.base-path=/` | 175 |
| gateway | maven | none | `/gateway/health` (otel ignore URL) | 168 |
| user-otp | maven | `/user-otp` | none | 165 |
| egov-document-uploader | maven | `/egov-document-uploader` | none | 156 |
| egov-searcher | maven | `/egov-searcher` | none | 155 |
| egov-enc-service | maven | `/egov-enc-service` | none | 153 |
| xstate-chatbot/nodejs | node | none | none | 141 |
| xstate-chatbot/react-app | node | none | none | 141 |
| tenant | maven | none | none | 126 |
| egov-url-shortening | maven | `/egov-url-shortening` | none | 122 |
| egov-notification-mail | maven | none | none | 116 |
| individual | maven | `/individual` | none | 101 |
| egov-telemetry/egov-telemetry-batch-process | maven | none | none | 98 |
| egov-telemetry/egov-telemetry-kafka-streams | maven | none | none | 98 |
| egov-telemetry/telemetry-client | node | none | none | 98 |
| egov-mdms-service | maven | `/egov-mdms-service` | `management.endpoints.web.base-path=/` | 79 |
| national-dashboard-kafka-pipeline | maven | `/national-dashboard` | none | 61 |
| upyog-draft-service | maven | `/upyog-draft-service` | none | 57 |
| gis-service | maven | `/gis-service` | none | 44 |
| nlp-engine | node | none | none | 25 |

Checked-in folders that did not have `pom.xml` or `package.json` were not catalogued (`upyog-dpdp-service` has source and no build file). `build-config.yml` names `internal-gateway`, `egov-user-event`, `http-to-kafka-connector`, and `mailbot`; those directories are not in this tree.

The runnable catalog is `core-smoke-tests/src/test/resources/services.yaml`, executed by `CoreServicesSmokeTest`.
