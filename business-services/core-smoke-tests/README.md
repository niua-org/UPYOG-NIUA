# core-smoke-tests

Black-box HTTP smoke tests for UPYOG **business-services**. No dependency on service source code.

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ENVIRONMENT` | yes | Target env label (e.g. `uat`, `prod`) |
| `BASE_URL` | yes | Gateway base URL (no trailing slash) |
| `SMOKE_TENANT_ID` | yes | Dedicated smoke tenant |
| `SMOKE_USER` / `SMOKE_PASSWORD` | one of | OAuth password grant against `/user/oauth/token` |
| `SMOKE_TOKEN` | one of | Pre-minted bearer token |
| `EXPECTED_VERSION` | no | If set, version endpoint must contain this string |
| `SERVICE` | no | Service name or `ALL` (default `ALL`) |

Maven: `-Dservice=billing-service` maps to `-Dservice` system property (Surefire).

## Run locally

```bash
export ENVIRONMENT=local
export BASE_URL=https://your-gateway.example.org
export SMOKE_TENANT_ID=pg.smoke
export SMOKE_USER=smoke.tester
export SMOKE_PASSWORD=***
mvn -f business-services/core-smoke-tests/pom.xml -Psmoke verify
```

Single service:

```bash
mvn -f business-services/core-smoke-tests/pom.xml -Psmoke verify -Dservice=billing-service
```

## Add a service

1. Add entry to `src/test/resources/services.yaml`.
2. Add `src/test/java/org/upyog/smoke/<pkg>/<Service>SmokeTest.java` extending `ServiceSmokeTestTemplate`.

## Smoke tenant seed

See `scripts/setup-smoke-tenant.md`.

## Jenkins

Pipeline: `business-services/core-smoke-tests/Jenkinsfile`

Credential IDs (configure in Jenkins):

| ID | Purpose |
|----|---------|
| `smoke-oauth-user` | `SMOKE_USER` |
| `smoke-oauth-password` | `SMOKE_PASSWORD` |
| `smoke-tenant-id` | `SMOKE_TENANT_ID` |
| `smoke-base-url-<env>` | `BASE_URL` per environment |

## Troubleshooting

Failures include HTTP status, body excerpt (500 chars), and `x-correlation-id` when present. Secrets are never logged.
