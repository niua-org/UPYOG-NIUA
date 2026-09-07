package org.upyog.dashboard.repository.querybuilder;

/**
 * Query constants class for SQL queries related to ingestion summary
 * persistence. Defines immutable, reusable SQL query strings.
 */
public final class IngestionSummaryQueryBuilder {

    /**
     * Private constructor to prevent instantiation of static query constant
     * holder class.
     */
    private IngestionSummaryQueryBuilder() {
        // Prevent instantiation
    }

    /**
     * Query to retrieve the last successful ingestion date for a specific
     * tenant and module.
     */
    public static final String SELECT_LAST_SUCCESSFUL_DATE_QUERY
            = "SELECT last_successful_date FROM ingestion_module_summary WHERE tenant_id = :tenantId AND module_name = :moduleName";

    /**
     * Query to retrieve all last successful ingestion dates for all tenants for a module in a single bulk query.
     */
    public static final String SELECT_ALL_LAST_SUCCESSFUL_DATES_FOR_MODULE_QUERY
            = "SELECT tenant_id, last_successful_date FROM ingestion_module_summary WHERE module_name = :moduleName";

    /**
     * Query to retrieve all distinct tenant IDs that have already successfully ingested for a specific target date.
     */
    public static final String SELECT_TENANTS_SUCCESSFULLY_INGESTED_FOR_DATE_QUERY
            = "SELECT DISTINCT tenant_id FROM ("
            + "  SELECT tenant_id FROM ingestion_module_summary WHERE module_name = :moduleName AND last_successful_date >= :targetDate "
            + "  UNION "
            + "  SELECT tenant_id FROM ingestion_detail WHERE module_name = :moduleName AND push_date = :targetDate AND ingestion_status = 'SUCCESS' "
            + "  UNION "
            + "  SELECT tenant_id FROM legacy_data_ingestion_detail WHERE module_name = :moduleName AND push_date = :targetDate AND ingestion_status = 'SUCCESS' "
            + ") completed_tenants";

    /**
     * Query to insert or update the last successful and attempted ingestion
     * dates in ingestion_module_summary.
     */
    public static final String UPSERT_LAST_SUCCESSFUL_DATE_QUERY
            = "INSERT INTO ingestion_module_summary ("
            + "   id, tenant_id, module_name, last_successful_date, last_attempted_date, created_by, last_modified_by"
            + ") VALUES (:id, :tenantId, :moduleName, :lastSuccessfulDate, :lastAttemptedDate, :createdBy, :lastModifiedBy) "
            + "ON CONFLICT (tenant_id, module_name) "
            + "DO UPDATE SET last_successful_date = EXCLUDED.last_successful_date, "
            + "              last_attempted_date = EXCLUDED.last_attempted_date, "
            + "              last_modified_by = EXCLUDED.last_modified_by, "
            + "              last_modified_time = (ROUND(EXTRACT(EPOCH FROM NOW()) * 1000))::bigint";

    /**
     * Query to insert or update the last attempted ingestion date in
     * ingestion_module_summary.
     */
    public static final String UPSERT_LAST_ATTEMPTED_DATE_QUERY
            = "INSERT INTO ingestion_module_summary ("
            + "   id, tenant_id, module_name, last_successful_date, last_attempted_date, created_by, last_modified_by"
            + ") VALUES (:id, :tenantId, :moduleName, :lastSuccessfulDate, :lastAttemptedDate, :createdBy, :lastModifiedBy) "
            + "ON CONFLICT (tenant_id, module_name) "
            + "DO UPDATE SET last_attempted_date = EXCLUDED.last_attempted_date, "
            + "              last_modified_by = EXCLUDED.last_modified_by, "
            + "              last_modified_time = (ROUND(EXTRACT(EPOCH FROM NOW()) * 1000))::bigint";

    /**
     * Query to retrieve all distinct dates successfully ingested within a date
     * range across daily and legacy tables.
     */
    public static final String SELECT_SUCCESSFUL_DATES_IN_RANGE_QUERY
            = "SELECT DISTINCT push_date FROM ("
            + "  SELECT push_date FROM ingestion_detail WHERE tenant_id = :tenantId AND module_name = :moduleName AND ingestion_status = 'SUCCESS' AND push_date >= :startDate AND push_date <= :endDate "
            + "  UNION "
            + "  SELECT push_date FROM legacy_data_ingestion_detail WHERE tenant_id = :tenantId AND module_name = :moduleName AND ingestion_status = 'SUCCESS' AND push_date >= :startDate AND push_date <= :endDate "
            + ") combined_dates";

    /**
     * Query to update the module detail table modified timestamp.
     */
    public static final String UPDATE_MODULE_DETAIL_TABLE_QUERY
            = "UPDATE ingestion_module_detail SET last_modified_time = (ROUND(EXTRACT(EPOCH FROM NOW()) * 1000))::bigint "
            + "WHERE tenant_id = :tenantId AND module_name = :moduleName";

    /**
     * Query to retrieve all distinct push dates registered in
     * legacy_data_ingestion_detail for a tenant and module.
     */
    public static final String SELECT_LEGACY_JOB_DATES_QUERY
            = "SELECT DISTINCT push_date FROM legacy_data_ingestion_detail WHERE tenant_id = :tenantId AND module_name = :moduleName";

    /**
     * Query to insert a new legacy job record into
     * legacy_data_ingestion_detail.
     */
    public static final String INSERT_LEGACY_JOB_QUERY
            = "INSERT INTO legacy_data_ingestion_detail ("
            + "   module_ingestion_id, tenant_id, module_name, push_date, start_date, end_date, ingestion_status, exception_code, created_by, last_modified_by"
            + ") VALUES (:id, :tenantId, :moduleName, :pushDate, :startDate, :endDate, :status, :exceptionCode, :createdBy, :lastModifiedBy)";

    /**
     * Query to select pending or failed legacy jobs ordered by push date
     * ascending up to a specified limit.
     */
    public static final String SELECT_PENDING_OR_FAILED_LEGACY_JOBS_QUERY
            = "SELECT module_ingestion_id, push_date FROM legacy_data_ingestion_detail "
            + "WHERE tenant_id = :tenantId AND module_name = :moduleName AND ingestion_status IN ('NOT_STARTED', 'FAILURE') "
            + "ORDER BY push_date ASC LIMIT :limit";

    /**
     * Query to update the status and payload data of an existing legacy
     * ingestion job.
     */
    public static final String UPDATE_LEGACY_JOB_STATUS_QUERY
            = "UPDATE legacy_data_ingestion_detail "
            + "SET ingestion_status = :status, request_data = :requestData::jsonb, response_data = :responseData::jsonb, last_modified_time = (ROUND(EXTRACT(EPOCH FROM NOW()) * 1000))::bigint "
            + "WHERE module_ingestion_id = :id";

    /**
     * Query to acquire a row-level pessimistic lock (FOR UPDATE) on
     * ingestion_module_summary for a tenant and module.
     */
    public static final String SELECT_FOR_UPDATE_SUMMARY_QUERY
            = "SELECT id FROM ingestion_module_summary WHERE tenant_id = :tenantId AND module_name = :moduleName FOR UPDATE";

    /**
     * Query to find overlapping successful legacy jobs within a given date
     * window.
     */
    public static final String SELECT_OVERLAPPING_SUCCESSFUL_LEGACY_JOBS_QUERY
            = "SELECT module_ingestion_id, push_date, start_date, end_date "
            + "FROM legacy_data_ingestion_detail "
            + "WHERE tenant_id = :tenantId AND module_name = :moduleName AND ingestion_status = 'SUCCESS' "
            + "  AND ((start_date IS NOT NULL AND end_date IS NOT NULL AND start_date <= :endDate AND end_date >= :startDate) "
            + "       OR (push_date >= :startDate AND push_date <= :endDate))";

    /**
     * Query to insert a daily ingestion detail record into ingestion_detail.
     */
    public static final String INSERT_INGESTION_DETAIL_QUERY
            = "INSERT INTO ingestion_detail ("
            + "   module_ingestion_id, module_detail_id, tenant_id, module_name, push_date, request_data, response_data, ingestion_status, exception_code, created_by, last_modified_by"
            + ") VALUES (:moduleIngestionId, :moduleDetailId, :tenantId, :moduleName, :pushDate, :requestData::jsonb, :responseData::jsonb, :ingestionStatus, :exceptionCode, :createdBy, :lastModifiedBy)";

    /**
     * Query to upsert a module detail record into ingestion_module_detail.
     */
    public static final String UPSERT_MODULE_DETAIL_QUERY
            = "INSERT INTO ingestion_module_detail ("
            + "   detail_id, tenant_id, module_name, is_active, created_by, created_time, last_modified_by, last_modified_time"
            + ") VALUES (:detailId, :tenantId, :moduleName, :isActive, :createdBy, :createdTime, :lastModifiedBy, :lastModifiedTime) "
            + "ON CONFLICT (detail_id) "
            + "DO UPDATE SET is_active = EXCLUDED.is_active, last_modified_by = EXCLUDED.last_modified_by, "
            + "              last_modified_time = EXCLUDED.last_modified_time";

    /**
     * Query to select distinct active tenant IDs for a specific module.
     */
    public static final String SELECT_ACTIVE_TENANTS_BY_MODULE_QUERY
            = "SELECT DISTINCT tenant_id FROM ingestion_module_detail WHERE is_active = TRUE AND module_name = :moduleName ORDER BY tenant_id ASC";

    /**
     * Query to select all distinct active tenant IDs across all modules.
     */
    public static final String SELECT_ALL_ACTIVE_TENANTS_QUERY
            = "SELECT DISTINCT tenant_id FROM ingestion_module_detail WHERE is_active = TRUE ORDER BY tenant_id ASC";

    /**
     * Query to select all active module detail records.
     */
    public static final String SELECT_ALL_ACTIVE_MODULE_DETAILS_QUERY
            = "SELECT detail_id, tenant_id, module_name, is_active FROM ingestion_module_detail WHERE is_active = TRUE ORDER BY tenant_id, module_name";

    /**
     * Query to delete all records from ingestion_module_detail.
     */
    public static final String DELETE_ALL_MODULE_DETAILS_QUERY
            = "DELETE FROM ingestion_module_detail";

    /**
     * Query to check if legacy ingestion has completed successfully for a given tenant and module.
     */
    public static final String CHECK_LEGACY_INGESTION_COMPLETE_QUERY
            = "SELECT COUNT(*) FROM legacy_data_ingestion_detail WHERE tenant_id = :tenantId AND module_name = :moduleName AND ingestion_status = 'SUCCESS'";
}

