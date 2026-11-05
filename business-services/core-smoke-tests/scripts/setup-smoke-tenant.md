# Smoke tenant setup

Create an isolated tenant and user for post-deploy smoke tests. **Do not** use production citizen tenants.

1. Create tenant `pg.smoke` (or your org prefix) in tenant service.
2. Create employee user `smoke.tester@niua.org` with roles sufficient for billing, collection, HRMS search APIs.
3. Prefix any created business data with `SMOKE_`.
4. Store credentials in Jenkins (`smoke-oauth-user`, `smoke-oauth-password`, `smoke-tenant-id`).
5. Re-run smokes; write flows should create -> search -> verify only within `pg.smoke`.

Cleanup: delete `SMOKE_*` entities periodically via admin APIs or DB scripts in non-prod.
