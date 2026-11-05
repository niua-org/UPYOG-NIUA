# Seed the smoke tenant

Do not commit tenant codes, usernames, passwords, tokens, or base URLs. This file is setup steps only. Smoke tests were not executed against a live environment.

## 1. Pick the environment

Use the same `ENVIRONMENT` value the Jenkins job will pass (`dev`, `qa`, `uat`, or `prod`). The name does not imply a host. The host is Jenkins credential `smoke-base-url`, bound to `BASE_URL`.

## 2. Create a dedicated tenant

Create one tenant used only for smoke traffic. Put its code in Jenkins parameter `SMOKE_TENANT_ID` (or the job environment). Do not point smoke tests at a production citizen tenant.

The tenant service's context path is not set in `application.properties`, so this guide does not name a create URL. Use the environment's existing tenant-admin process.

## 3. Create the smoke user

Create a user that belongs only to that tenant. Store the username and password in Jenkins credential `smoke-user-credentials` (username/password). That credential binds `SMOKE_USER` and `SMOKE_PASSWORD`.

If you already have a bearer token, inject it as `SMOKE_TOKEN` instead. Do not store the token in git.

Password grant also needs `SMOKE_USER_TYPE` and `SMOKE_AUTH_PATH`. `SMOKE_AUTH_PATH` is the token path on `BASE_URL`. Leave it unset unless that path has been confirmed for the target gateway. `AuthClient` will not guess a URL.

## 4. Jenkins credentials

| Credential id | Type | Bound variable |
| --- | --- | --- |
| `smoke-base-url` | secret text | `BASE_URL` |
| `smoke-user-credentials` | username/password | `SMOKE_USER`, `SMOKE_PASSWORD` |

`ENVIRONMENT`, `SERVICE` (default `ALL`), `EXPECTED_VERSION`, and `SMOKE_TENANT_ID` are job parameters, not credential ids.

## 5. What the suite will call

`services.yaml` enables a health GET only for `gateway` at `/gateway/health` (from `otel.instrumentation.http.server.ignore-urls` in gateway `application.properties`). Every other health, version, read, write, auth, and tenant check is skipped until its path is filled from config. No write is sent by the current catalog.

## 6. Schedule

`core-smoke-tests/Jenkinsfile` runs every half hour with `SERVICE=ALL`. Confirm `SMOKE_TENANT_ID` and `smoke-base-url` are set on that job before enabling the cron in Jenkins.
