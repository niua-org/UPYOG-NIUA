# State Manual for Ingestion Pipeline

## Architecture Overview

The Ingestion Pipeline is responsible for extracting, transforming, validating, and loading metrics from local state services into the National Dashboard. It is split into two primary components:

1. **dashboard-data-extractor**: Contains module-specific extractor logic that fetches metrics data from database/APIs and passes it to the generic transformation layer. Includes `DailyIngestionScheduler` and `LegacyIngestionScheduler`.
2. **dashboard-data-engine**: Contains module-agnostic transformation and loading abstractions. Uses standard transformers (`PTTransformer`, `PGRTransformer`) and validators (`PTValidator`, `PGRValidator`) to massage raw metric structures into `NationalDashboardIngestRequest` and push to the ingest endpoint using `HttpLoader`.

The system persists logs to a database via `egov-persister`, making use of Kafka topics (`save-adapter-ingestion-detail`, `save-adapter-module-ingestion-detail`, `save-adapter-error-log`).

## Persistence Strategy

The service supports two persistence modes, toggled via `dashboard-data.persister.enabled`:

| Mode | Value | Implementation class |
|------|-------|----------------------|
| **Kafka (default)** | `true` | `KafkaIngestionPersistenceServiceImpl` — publishes to Kafka topics; `egov-persister` consumes and writes to DB. |
| **JDBC (direct)** | `false` | `JdbcIngestionPersistenceServiceImpl` — writes to DB directly via `JdbcTemplate`. |

Both implementations satisfy the `IngestionPersistenceService` interface and are loaded conditionally via `@ConditionalOnProperty`. All fields use constructor injection (`@RequiredArgsConstructor`).

## Ingestion Statuses & Enum Mapping

The pipeline uses the type-safe `IngestionStatus` enum (`org.upyog.dashboard.enums.IngestionStatus`) to evaluate ingestion execution outcomes:

| Status Enum | Description / Behavior |
| :--- | :--- |
| `SUCCESS` | Ingestion succeeded and data was pushed to the National Dashboard API. |
| `SUCCESS_ZERO_METRICS` | All extracted metrics for the date are zero. Downstream HTTP push is skipped to save bandwidth, but module tracker date advances. |
| `SUCCESS_DUPLICATE` | The target date was already ingested (`EG_DS_RECORD_ALREADY_INGESTED_ERR`). Handled as success so module tracker advances. |
| `FAILURE` | Ingestion failed (HTTP 4xx/5xx, timeout, or database exception). Halts catch-up loop. |
| `SKIPPED` | Ingestion was skipped (e.g. module already up to date). |
| `UNKNOWN` | Fallback status for unrecognized status strings (`@JsonCreator` fallback). |

Both `SUCCESS`, `SUCCESS_ZERO_METRICS`, and `SUCCESS_DUPLICATE` return `isSuccess() = true`, enabling `last_successful_date` in `ug_ingestion_module_summary` to advance cleanly.

## Storage & S3 Folder Hierarchy

When files are generated and uploaded to AWS S3 (or FileStore), the object key is built via `CommonUtils.buildS3Key(folder, tenantId, moduleName, fileName)`.

The folder structure is organized as follows:

### 1. Daily Ingestion: State-Level Grouping
- **Hierarchy Pattern:**
  `<awsS3Folder>/<state>/<module>/<uuid>_daily_<module>_<timestamp>.xlsx`
- **Parent Folder:** **State Code** (e.g. `pg`).
  - In `S3DashboardDataLoaderImpl`, the ULB identifier (e.g. `pg.citya`) is split at `.` (`payloadUlb.split("\\.")[0]`) to resolve the parent state code.
- **Inner Folder:** **Module Code** (e.g. `PT`, `PGR`, `CHB`).
- **File Prefix:** `daily_` (defined in `DashboardConstants.DAILY`).
- **Sheet Name:** `{MODULE}_daily`.
- **Example S3 Key:**
  `dashboard/pg/PT/a89f41b2-3f1d-4b89-9a07-8e6f3328dc41_daily_PT_1725960000.xlsx`

### 2. Legacy Batch Ingestion: ULB-Level Grouping
- **Hierarchy Pattern:**
  `<awsS3Folder>/<ulb>/<module>/<uuid>_legacy_<module>_<timestamp>.xlsx`
- **Parent Folder:** **ULB Tenant ID** (e.g. `pg.citya`).
  - In `LegacyBatchIngestionOrchestrator`, the full tenant ID from the request or active tenant list (`request.getTenantId()`) is used without splitting.
- **Inner Folder:** **Module Code** (e.g. `PT`, `PGR`, `CHB`).
- **File Prefix:** `legacy_` (defined in `DashboardConstants.LEGACY`).
- **Sheet Name:** `{MODULE}_legacy`.
- **Example S3 Key:**
  `dashboard/pg.citya/PT/b91c73e1-4c2e-4e90-8b18-7f5e2217cb32_legacy_PT_1725960000.xlsx`

### Summary Comparison

| Ingestion Pipeline | Parent Folder | Inner Subfolder | File Prefix | Sheet Name | Example Path |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Daily** | **State** (`pg`) | **Module** (`PT`) | `daily_` | `{MODULE}_daily` | `dashboard/pg/PT/<uuid>_daily_PT_...xlsx` |
| **Legacy** | **ULB** (`pg.citya`) | **Module** (`PT`) | `legacy_` | `{MODULE}_legacy` | `dashboard/pg.citya/PT/<uuid>_legacy_PT_...xlsx` |

## Database Schema

### Tables

| Table | Purpose |
|-------|---------|
| `ug_ingestion_module_detail` | Active ULB-module mapping registry synchronized from MDMS |
| `ug_ingestion_detail` | Daily ingestion detail records per module/date |
| `ug_legacy_data_ingestion_detail` | Legacy (historical daily) ingestion detail records |
| `ug_ingestion_module_summary` | Tracks last successful and last attempted date per tenant/module |
| `ug_adapter_ingestion_error_log` | Error log for ingestion pipeline issues |

### Schema Evolutions & Migrations

- **`exception_code` Column (`V20260818140000`):** Both `ug_ingestion_detail` and `ug_legacy_data_ingestion_detail` carry an `exception_code VARCHAR(128)` column to record short failure codes when `ingestion_status = FAILURE`.
- **Streamlined `ug_ingestion_module_detail` (`V20260907150000`):** Dropped legacy flags (`is_legacy_data_ingested`, `last_ingested_date`, `ulb_name`, `schedule_cron`). The table now functions purely as an active registry mapping ULBs to enabled modules, with legacy progress tracked directly in `ug_legacy_data_ingestion_detail` and `ug_ingestion_module_summary`.

## Multi-Tenant & MDMS Synchronization

### `TenantSyncService` & `TenantController`
- Synchronizes active city/ULB tenant IDs from eGov MDMS (`tenant` module, `nationalInfo` master) into `ug_ingestion_module_detail`.
- Manages in-memory caching via `@Cacheable` and `@CacheEvict` using centralized cache names (`active_tenants`, `tenant_module_details`).
- Provides REST endpoints:
  - `POST /tenant/sync?stateTenantId={state}` — Triggers MDMS pull and atomically refreshes `ug_ingestion_module_detail`.
  - `GET /tenant/search` (or `/_search`) — Returns active tenants and module mappings from cache/database.

## Key Service Classes

### `DailyIngestionService`
- Fetches active ULB tenants per module via `TenantSyncService.getActiveTenants(...)`.
- Queries `ug_ingestion_module_summary.findAllLastSuccessfulDatesByModule(...)` to bulk fetch checkpoints for all tenants in a single query.
- Executes **multi-tenant batch extraction** (`ModuleExtractor.extractData(List<String> tenantIds, LocalDate targetDate)`) using parameterized SQL queries with `UNNEST(string_to_array(:tenantId, ','))` across configured batch chunks (`dashboard-data.extractor.tenant-batch-size`).
- Performs **catch-up ingestion** across missing date ranges up to yesterday, automatically handling and advancing zero-metric tenants.
- Employs reflection caching (`ConcurrentHashMap`) in `extractTenantId` to eliminate runtime reflection overhead.
- Uses `saveOrUpdateLastAttemptedDatesBatch` to eliminate N+1 database roundtrips during catch-up iterations.

### `LegacyIngestionService`
- Manages bulk historical ingestion via a **two-phase scheduler** approach:
  1. **Populate phase** (`populateLegacyJobs` / `populateLegacyJobsForRange`): Determines which dates in the given range have not yet been ingested and creates `NOT_STARTED` rows in `ug_legacy_data_ingestion_detail`.
  2. **Execute phase** (`executeLegacyJobs`): Fetches pending/failed legacy job rows and runs them through the extractor + dashboard client pipeline.
- Checks legacy completion via `IngestionSummaryRepository.isLegacyIngestionComplete(...)` querying `ug_legacy_data_ingestion_detail`.
- Extracts logic into private helpers: `processLegacyJob(...)` for ingestion execution, `serializeRequest(...)` for JSON payload, and `sanitizeResponse(...)`/`sanitizeJson(...)` for safe JSONB storage.
- Uses `@RequiredArgsConstructor` constructor injection instead of `@Autowired` field injection.
- Persistence is fully delegated to `IngestionPersistenceService` (supporting both Kafka and direct JDBC modes).

### `IngestionSummaryRepository`
- Queries `ug_ingestion_module_summary` for the last successful date and last attempted date per tenant/module.
- `findSuccessfullyIngestedDates(...)` performs a UNION query across both `ug_ingestion_detail` and `ug_legacy_data_ingestion_detail` to determine already-ingested dates in a range.
- `isLegacyIngestionComplete(...)` verifies legacy completion status from `ug_legacy_data_ingestion_detail`.
- Supports batch attempted date updates via `saveOrUpdateLastAttemptedDatesBatch(...)`.
- All persistence side-effects are delegated to `IngestionPersistenceService` (not direct JDBC writes).

### `IngestionSummaryQueryBuilder`
- Central SQL query factory for all queries against `ug_ingestion_module_summary`, `ug_ingestion_module_detail`, and `ug_legacy_data_ingestion_detail`.
- All query builder methods and constants are documented with Javadoc describing parameters and behavior.

## Utility Classes

### `CommonUtils`
- Provides `getCurrentEpochMillis()` — a single source of truth for timestamps across all persistence operations.

### `HierarchyParser`
- Spring component that parses a dot-notation tenant ID (`state.ulb[.region[.ward]]`) into a `Map<String, String>` of hierarchy levels.
- Default ward and region values are injected from `dashboard-data.metric.ward` and `dashboard-data.metric.region` application properties.
- Used by extractors that need to populate metric hierarchy fields in the `NationalDashboardIngestRequest`.

## Onboarding a New State / Module

### 1. Extractors
- Create a new Extractor class implementing `ModuleExtractor<T>` inside `dashboard-data-extractor` (`org.upyog.dashboard.extractor.impl`).
- Register it using the `@Component` annotation so that `ExtractorRegistry` automatically picks it up based on its implemented generic module type.

### 2. Transformers
- Implement `ModuleTransformer<T>` inside `dashboard-data-engine` (`org.upyog.adapter.transformer.impl`).
- The `TransformerRegistry` automatically maps the Module ENUM to this transformer.

### 3. Validators
- Implement `ModuleValidator<T>` inside `dashboard-data-engine` (`org.upyog.adapter.validator.impl`).
- The `ValidatorRegistry` automatically handles execution before transformation.

### 4. Application Properties
Enable the new module in `application.properties`:
```properties
extractor.enabled-modules=PT,PGR,NEW_MODULE
```

### 5. DB Migration (Optional)
If your state requires new tracking tables, place your migration scripts in `dashboard-data-extractor/src/main/resources/db/migration/main/`. Ensure the new tables are tied to the Kafka producer config within `adapter-service-persister.yml`.

### 6. Legacy Ingestion
You can ingest historical data using the newly built API:
`POST /api/v1/legacy/ingest?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&module=NEW_MODULE`
Check the job statuses using:
`GET /api/v1/legacy/jobs/status?tenantId=pg&moduleName=NEW_MODULE`

## Coding Conventions

- All Spring beans use **constructor injection** via `@RequiredArgsConstructor` (Lombok). Do not use `@Autowired` field injection.
- Repeated string constants (`"SYSTEM"`, `"SUCCESS"`, `"FAILURE"`) are extracted into `private static final` fields within each class.
- Log messages do **not** repeat the class name prefix (e.g., avoid `"ClassName | message"`); the MDC/log format provides the class context automatically.
- Repeated `DateTimeFormatter` instances are stored as `private static final` fields instead of being created inline.
