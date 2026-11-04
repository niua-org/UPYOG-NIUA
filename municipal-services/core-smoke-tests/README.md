# core-smoke-tests

Shared **post-deploy** HTTP smoke suite for `municipal-services`. Black-box only — no dependency on service bytecode (Java and Node services are exercised the same way).

## Required environment variables

| Variable | Description |
|----------|-------------|
| `ENVIRONMENT` | `dev`, `uat`, or `prod` |
| `BASE_URL` | API gateway root (no trailing slash) |
| `SMOKE_TENANT_ID` | Dedicated smoke tenant (city-level, not state-level) |
| `SMOKE_USER` / `SMOKE_PASSWORD` | Smoke employee user, or use `SMOKE_AUTH_TOKEN` instead |
| `EXPECTED_VERSION` | Optional; when set, version/info endpoint must contain this string |
| `SERVICE` / `-Dservice=` | Service name from `services.yaml`, `e2e`, or `ALL` (default) |

Optional: `SMOKE_ISOLATION_TENANT_ID`, `SMOKE_HTTP_TIMEOUT_SECONDS`, `SMOKE_HEALTH_WARMUP_SECONDS`, `SMOKE_TOKEN_PATH`, `SMOKE_USER_TYPE`.

## Run locally

```bash
cd municipal-services/core-smoke-tests
export ENVIRONMENT=dev
export BASE_URL=https://your-gateway.example
export SMOKE_TENANT_ID=pg.citya
export SMOKE_USER=smoke.employee
export SMOKE_PASSWORD=***
mvn -Psmoke -Dservice=property-services test
```

All services:

```bash
mvn -Psmoke -Dservice=ALL test
```

## Add a service

1. Add an entry to `src/test/resources/services.yaml`.
2. Create `src/test/java/org/niua/smoke/<pkg>/<Service>SmokeTest.java` extending `AbstractServiceSmokeTest` with `@SmokeService("name")`.
3. Run with `-Dservice=<name>`.

## Jenkins

- Shared job: `municipal-services/core-smoke-tests/Jenkinsfile`
- Credential IDs (per environment): `smoke-<env>-tenant-id`, `smoke-<env>-user`, `smoke-<env>-password`, `smoke-<env>-base-url`
- After each service deploy, trigger this job with `SERVICE=<service>`, `ENVIRONMENT`, `EXPECTED_VERSION` (see `municipal-services/jenkins/deploy-with-smoke.groovy`).
- Schedule `SERVICE=ALL` every 30–60 minutes on UAT/prod for synthetic monitoring.

## Smoke tenant setup

See `scripts/smoke-tenant-setup.md`. All writes use `SMOKE_` prefixes and the dedicated tenant only.

## Troubleshooting

Failures print HTTP method, URL, status, masked secrets, body excerpt, and `x-correlation-id` when present.
