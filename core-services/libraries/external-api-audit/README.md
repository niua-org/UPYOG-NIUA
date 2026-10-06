# External API Audit Library

Reusable UPYOG library for **explicitly identified** external / third-party integration audit.

Persistence path is unchanged:

```
Service → ExternalApiAuditLogger.execute(...) → Kafka (async, fail-open)
        → egov-persister (external-api-audit-persister.yml)
        → ug_external_api_* tables
```

Do **not** use this to audit internal UPYOG APIs. Wrap only the specific inbound or outbound integration.

## Add a new integration

1. Depend on `org.egov.services:external-api-audit`.
2. Set properties (topic, source service, payload capture).
3. Wrap the call:

```java
externalApiAuditLogger.logAndExecute(
    ExternalIntegrationContext.builder()
        .tenantId("PFMS")
        .externalApiName("pfms-data-push")
        .requestPayload(request)
        .originatingCorrelationId(transaction.getCorrelationId())
        .businessReferenceId(transaction.getId())
        .endpoint(url)
        .httpMethod("POST")
        .retryCount(0)
        .build(),
    () -> feignClient.call(request));
```

Use `logInboundApi(...)` for inbound external calls (state → UPYOG).

A **new UUID** is generated per logical request (`correlation_id` unique). Pass the same `correlationId` and an incremented `retryCount` only when retrying the **same** request. A later new request (e.g. next-day scheduler) must get a new row.

## Schedulers

Jobs live in this library but **must be enabled on exactly one service** whose datasource is the database where Persister writes `ug_external_api_*` (currently National Dashboard Ingest).

| Job | Property | Purpose |
|---|---|---|
| Reconciliation | `external.api.audit.reconciliation.enabled=true` | `INITIATED` → `TIMED_OUT` after stale threshold |
| Cleanup | `external.api.audit.cleanup.enabled=true` | Delete rows older than retention |

Do not enable these in `data-sharing-service-dx` unless it shares that audit database.

## Payload / secrets

- `external.api.audit.capture-payload.enabled` turns request/response body capture on or off.
- Password, tokens, and bank-account field names are masked before persist.

## Testing

Table-backed integration tests and a step-by-step SQL checklist live in the consuming services:

- [National Dashboard Ingest test plan](../../national-dashboard-ingest/docs/EXTERNAL_API_AUDIT_TESTING.md)
- [Data Sharing Service (DX/PFMS) test plan](../../../dx-services/data-sharing-service-dx/docs/EXTERNAL_API_AUDIT_TESTING.md)

```bash
cd core-services/national-dashboard-ingest
mvn test -Dtest=MetricIngestAuditIntegrationTest,BatchIngestAuditIntegrationTest,ExternalApiAuditLifecycleIntegrationTest

cd dx-services/data-sharing-service-dx
mvn test -Dtest=PFMSInboundAuditIntegrationTest,PFMSOutboundAuditIntegrationTest,PFMSForwardingRetryAuditIntegrationTest
```
