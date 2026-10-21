# DX Services — Data Sharing Service

## Overview

`data-sharing-service-dx` is a Spring Boot microservice built on the eGov platform. It acts as the **UPYOG Data Exchange (DX) layer** — receiving PFMS (Public Financial Management System) transaction data from state systems, persisting it via the eGov persister, and forwarding it to the external PFMS API on a scheduled basis.

---

## Architecture

```
State System
     │
     ▼
[POST /pfms/v1/transactions/_create]
     │
     ▼
PFMSServiceImpl
  ├── validateDuplicates()       → checks DB for existing voucher numbers
  ├── PFMSDataEnricher.enrich()  → assigns IDs, status, serializes additionalDetails
  └── PFMSProducer.push()        → publishes to Kafka topic
          │
          ▼
    Kafka Topic (state-to-upyog-data-exchange)
          │
          ▼
    eGov Persister
          │
          ├── INSERT → ug_pfms_data
          └── INSERT → ug_pfms_transactions (additionaldetails as JSONB)
          
          
Scheduler (cron)
     │
     ▼
PFMSForwardingScheduler  →  PFMSForwardingServiceImpl
  ├── fetchInitiatedTransactionIds()
  ├── fetchAccessToken()          → PFMS Auth API
  ├── pushTransaction()           → PFMS Data API (with 401 token refresh)
  └── updateTransactionStatus()   → INITIATED → SUCCESS/FAILED
  
  
Retry Scheduler (separate cron)
     │
     ▼
PFMSRetryScheduler  →  PFMSForwardingServiceImpl.retryFailedTransactions()
```

---

## Key Design Decisions

### Flat DB Schema + JSONB additionalDetails
`ug_pfms_transactions` stores only 8 flat columns needed for querying/indexing:

| Column | Purpose |
|---|---|
| `id` | Primary key (UUID) |
| `correlation_id` | Batch tracking ID |
| `ulb_code_or_pfms_agency_code` | ULB identifier, part of unique constraint |
| `voucher_number` | Business key, part of unique constraint |
| `voucher_date` | Transaction date |
| `financial_year` | Financial year |
| `function_code` | Function code |
| `scheme_code` | Scheme code |
| `additionaldetails` | JSONB — all remaining fields packed here |

All other fields (voucherType, accountHeadCode, debitAmount, beneficiary details, etc.) are serialized into `additionaldetails` JSONB by `PFMSTransactionTransformer` before Kafka publish.

**Why String, not Map?**
The eGov persister uses JDBC `setObject` directly on values from the Kafka JSON. If `additionalDetails` is a `Map`, JDBC tries to bind it as PostgreSQL `hstore`, causing `PSQLException: No hstore extension installed`. Serializing to `String` before publish lets the persister bind it via `?::JSONB` cast cleanly.

### Unique Constraint
```sql
UNIQUE (ulb_code_or_pfms_agency_code, voucher_number)
```
A voucher number is unique per ULB/PFMS agency — not globally.

### Duplicate Validation (Two-Level)
1. **Intra-request** — checks for duplicate `(ulbCode + voucherNumber)` within the same incoming payload
2. **DB-level** — queries `ug_pfms_transactions` for any already-persisted voucher numbers for the same agency code

### Scheduler — Two Separate Jobs
| Scheduler | Cron Config | Purpose |
|---|---|---|
| `PFMSForwardingScheduler` | `pfms.scheduler.cron` | Picks `INITIATED` transactions, forwards to PFMS |
| `PFMSRetryScheduler` | `pfms.retry.scheduler.cron` | Picks `FAILED` transactions, retries forwarding |

Both schedulers share `PFMSForwardingServiceImpl.processBatch()` — only the fetch query differs.

### Token Refresh on 401
If PFMS returns 401 (token expired) mid-batch, the service refreshes the token and retries the **same transaction once** using the same `auditCorrelationId` — so the persister updates the existing audit row instead of inserting a duplicate.

---

## Request Flow

### POST `/pfms/v1/transactions/_create`

**Request:**
```json
{
  "RequestInfo": { "apiId": "...", "ver": "...", "msgId": "..." },
  "data": [
    {
      "state": "UP",
      "ward": "W01",
      "module": "PT",
      "date": "12-10-2026",
      "ulb": "AGRA",
      "targetDestination": "PFMS",
      "transactions": [
        {
          "ulbCodeOrPFMSAgencyCode": "AGR001",
          "voucherNumber": "VCH-2026-001",
          "voucherDate": "12/10/2026",
          "voucherType": "PAYMENT",
          "financialYear": "2026-27",
          "accountHeadCode": ["1234", "5678"],
          "functionCode": "FC01",
          "schemeCode": "SC01",
          "debitAmount": 5000.00
        }
      ]
    }
  ]
}
```

**Response:**
```json
{
  "responseInfo": {
    "apiId": "...",
    "ver": "...",
    "ts": 1728734447000,
    "msgId": "...",
    "resMsgId": "Request processed successfully",
    "status": "SUCCESS"
  },
  "correlationId": "uuid-of-the-batch",
  "transactions": [
    {
      "id": "uuid-of-transaction",
      "voucherNumber": "VCH-2026-001",
      "status": "SUCCESS"
    }
  ]
}
```

---

## Transaction Lifecycle

```
INITIATED  →  PROCESSING  →  SUCCESS
                          →  FAILED  →  (retry)  →  SUCCESS
                                                 →  FAILED
```

| Status | Set By | Meaning |
|---|---|---|
| `INITIATED` | `PFMSDataEnricher` | Persisted to DB, not yet forwarded to PFMS |
| `PROCESSING` | `PFMSForwardingServiceImpl` | Picked by scheduler, forwarding in progress |
| `SUCCESS` | `PFMSForwardingServiceImpl` | PFMS accepted the transaction |
| `FAILED` | `PFMSForwardingServiceImpl` | PFMS rejected or network error |

---

## Package Structure

```
pfms/
├── client/         → Feign clients for PFMS Auth + Data APIs
├── enricher/       → PFMSDataEnricher — assigns IDs, status, additionalDetails
├── models/         → Request/Response POJOs
├── producer/       → Kafka publisher
├── repository/     → JdbcTemplate queries + row mappers
├── scheduler/      → Cron-triggered forwarding and retry schedulers
└── service/
    ├── PFMSServiceImpl             → Ingestion flow (validate → enrich → publish)
    ├── PFMSForwardingServiceImpl   → Scheduler forwarding flow
    └── PFMSTransactionTransformer  → Serializes non-flat fields to JSON string
```

---

## Configuration Properties

```properties
# Kafka
pfms.kafka.topic=state-to-upyog-data-exchange

# Scheduler
pfms.scheduler.cron=0 0/5 * * * ?
pfms.retry.scheduler.cron=0 30 * * * ?
pfms.scheduler.batch-size=50

# PFMS External API
pfms.api.auth-url=...
pfms.api.data-url=...
pfms.api.client-id=...
pfms.api.client-secret=...
```

---

## Database Tables

| Table | Purpose |
|---|---|
| `ug_pfms_data` | One row per ingestion batch (state, ward, module, date, ulb) |
| `ug_pfms_transactions` | One row per transaction — flat columns + JSONB additionaldetails |
| `ug_pfms_scheduler_log` | Audit log for every scheduler cycle (success/failed counts, duration) |
