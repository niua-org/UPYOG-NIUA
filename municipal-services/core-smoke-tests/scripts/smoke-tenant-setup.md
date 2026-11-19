# Smoke tenant and user setup

1. Create tenant `SMOKE_TENANT_ID` (city-level, e.g. `pg.testing`) in MDMS tenant master — **not** production citizen tenants.
2. Create employee user `SMOKE_USER` with roles needed for municipal APIs (PGR, PT, WS, etc.).
3. Store credentials in Jenkins (`smoke-<env>-*` IDs listed in README).
4. Seed MDMS modules required by smoke read/write paths for that tenant (minimal ServiceDefs / connection types).
5. Re-run smokes; writes create data prefixed `SMOKE_` — safe to delete periodically.

Production: run write smokes **only** on the smoke tenant; read smokes may use broader tenants if policy allows.
