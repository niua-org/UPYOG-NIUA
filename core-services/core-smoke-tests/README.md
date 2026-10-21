# Core service smoke tests

Standalone Maven module. It is **not** a member of the core-services production reactor. Do not add `core-smoke-tests` to a parent `<modules>` list.

These tests were **not** executed against a live environment. Paths that are not confirmed in `application.properties`, `application.yml`, or a controller mapping are skipped with JUnit assumptions.

## What a check does

| Check | When it runs |
| --- | --- |
| Health is UP | Always, when `healthPath` is confirmed. Awaitility waits 60–90s. |
| Version equals `EXPECTED_VERSION` | Only when `EXPECTED_VERSION` is set and `versionPath` is confirmed. |
| Unauthenticated call returns 401 or 403 | Only when that service enforces auth. |
| One downstream call | Only when a downstream path is confirmed. |
| Core read returns 200 | Only when a read path is confirmed. |
| Write with `SMOKE_` prefix on the smoke tenant | Only when a create API is confirmed. |
| Invalid payload returns 400 | Status only, unless the real error JSON shape was read from code. |
| Tenant isolation | Only when the API is tenant-scoped. |

## Environment variables

| Variable | Required | Notes |
| --- | --- | --- |
| `ENVIRONMENT` | yes | `dev`, `qa`, `uat`, or `prod`. No URL is implied. |
| `BASE_URL` | yes | Gateway or ingress origin, no trailing path. |
| `SMOKE_TENANT_ID` | yes | Tenant used by every write and tenant-scoped read. |
| `SMOKE_TOKEN` | one of | Static bearer token. |
| `SMOKE_USER` + `SMOKE_PASSWORD` | one of | Used when `SMOKE_TOKEN` is absent. |
| `EXPECTED_VERSION` | no | When unset, version equality is skipped. |
| `SERVICE` | no | Service folder name, or `all` (default). Also accepted as `-Dservice=`. |

Jenkins credential IDs (bind these; do not commit values):

- `smoke-base-url` — secret text or string credential for `BASE_URL`
- `smoke-user-credentials` — username/password credential for `SMOKE_USER` / `SMOKE_PASSWORD`

`SMOKE_TENANT_ID` and `ENVIRONMENT` are job parameters, not secrets.

## Run

Without `-Psmoke`, Surefire and Failsafe skip every test.

```bash
export ENVIRONMENT=dev
export BASE_URL=https://<gateway-host>
export SMOKE_TENANT_ID=<smoke-tenant>
export SMOKE_USER=<user>
export SMOKE_PASSWORD=<password>
# optional
export EXPECTED_VERSION=1.2.3
export SERVICE=egov-user

cd core-services/core-smoke-tests
mvn -Psmoke verify -Dservice="${SERVICE:-all}"
```

## How to add a service

1. Confirm the folder is a deployable service (`pom.xml` or `package.json`) and is not `build/`, `docs/`, `libraries/`, or this module.
2. Read context path and health path only from `application.properties` or `application.yml`. If health is not there, look for a controller `@RequestMapping` / `@GetMapping` before writing a path. If you cannot confirm a path, leave it empty in `services.yaml` and skip that check with `Assumptions`.
3. Add one class `src/test/java/org/egov/core/smoke/<service>/<Service>SmokeTest.java` with `@Tag("smoke")`.
4. Add one `services.yaml` entry: `name`, `basePath`, `healthPath`, `versionPath`, `dependsOn`.
5. Do not invent create, search, or auth paths. Document unconfirmed checks as skipped in the test class.
6. Add a cross-service journey only after both sides of the call are confirmed. Otherwise record it as deferred in this README.

## Catalog

Service list, confirmed paths, and deferred journeys are in `src/test/resources/services.yaml` and `docs/test-strategy/PHASE0-DISCOVERY.md` (repo `core-services/docs/test-strategy/`).

Cross-service journeys are **deferred**. No pair of request paths was confirmed end-to-end in controller mappings during this pass, so no journey class calls a live API. See the deferred section at the bottom of this file after discovery is written; the generator keeps that section in sync.

## Troubleshooting

- **Build runs zero tests.** You omitted `-Psmoke`. Default `skipTests` and `excludedGroups=smoke` are intentional.
- **`Missing required environment variable`.** `SmokeConfig` fails fast. Set `ENVIRONMENT`, `BASE_URL`, `SMOKE_TENANT_ID`, and credentials before Surefire starts.
- **Health never becomes UP.** Awaitility polls up to 90 seconds. Check the gateway route for `basePath + healthPath` from `services.yaml`. The failure log prints method, URL, status, body excerpt, and `X-Correlation-Id`, with secrets masked.
- **401 on health.** Health is called without a token. If the gateway requires auth for actuator, the health check is the wrong contract — confirm the path in config before changing the test.
- **Version mismatch.** Comparison runs only when `EXPECTED_VERSION` is set. Unset it to skip.
- **Wrong service executed.** `SERVICE` / `-Dservice` must match the `name` in `services.yaml` (case-insensitive). `all` runs every class; each class still assumptions-skips when its own paths are empty.
- **Tests were not executed against a live environment.** A green local `mvn -Psmoke` without `BASE_URL` is not a deploy signal. Jenkins must inject `smoke-base-url` and `smoke-user-credentials`.

## Deferred cross-service journey

No journey class is included. Confirm both the caller path and the callee path in source, then add `org.egov.core.smoke.journey` with a single `@Tag("smoke")` class. Until then this item stays deferred.
