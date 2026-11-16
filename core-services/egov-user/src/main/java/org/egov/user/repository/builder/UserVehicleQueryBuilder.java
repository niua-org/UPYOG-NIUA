package org.egov.user.repository.builder;

import org.springframework.stereotype.Component;

/**
 * SQL Query Builder component generating persistence queries for citizen vehicle declarations
 * targeting {@code ug_user_vehicle} and historical auditing table {@code ug_user_vehicle_audit}.
 * Follows the standard UPYOG QueryBuilder architectural pattern.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class UserVehicleQueryBuilder {

    public static final String INSERT_VEHICLE_SQL =
            "INSERT INTO ug_user_vehicle (" +
            "id, user_uuid, tenant_id, vehicle_number, purchase_date, is_verified, validation_source, " +
            "rc_doc_id, vehicle_type, maker_model, fuel_type, category, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":id, :user_uuid, :tenant_id, :vehicle_number, :purchase_date, :is_verified, :validation_source, " +
            ":rc_doc_id, :vehicle_type, :maker_model, :fuel_type, :category, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (id) DO UPDATE SET " +
            "vehicle_number = EXCLUDED.vehicle_number, " +
            "purchase_date = EXCLUDED.purchase_date, " +
            "is_verified = EXCLUDED.is_verified, " +
            "validation_source = EXCLUDED.validation_source, " +
            "rc_doc_id = EXCLUDED.rc_doc_id, " +
            "vehicle_type = EXCLUDED.vehicle_type, " +
            "maker_model = EXCLUDED.maker_model, " +
            "fuel_type = EXCLUDED.fuel_type, " +
            "category = EXCLUDED.category, " +
            "status = EXCLUDED.status, " +
            "additional_details = EXCLUDED.additional_details, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time " +
            // Security: scope conflict update to owner user_uuid to prevent cross-user vehicle overwrites
            "WHERE ug_user_vehicle.user_uuid = EXCLUDED.user_uuid";

    public static final String INSERT_VEHICLE_AUDIT_SQL =
            "INSERT INTO ug_user_vehicle_audit (" +
            "audit_id, id, user_uuid, tenant_id, vehicle_number, purchase_date, is_verified, validation_source, " +
            "rc_doc_id, vehicle_type, maker_model, fuel_type, category, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":audit_id, :id, :user_uuid, :tenant_id, :vehicle_number, :purchase_date, :is_verified, :validation_source, " +
            ":rc_doc_id, :vehicle_type, :maker_model, :fuel_type, :category, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ")";

    public static final String SELECT_ACTIVE_VEHICLE_ID_BY_USER_AND_NUMBER_SQL =
            "SELECT id FROM ug_user_vehicle WHERE user_uuid = :user_uuid AND UPPER(vehicle_number) = UPPER(:vehicle_number) AND status = 'ACTIVE' LIMIT 1";

    public static final String SELECT_VEHICLE_USER_UUID_BY_ID_SQL =
            "SELECT user_uuid FROM ug_user_vehicle WHERE id = :id";

    public static final String SELECT_VEHICLES_BY_USER_BASE_SQL =
            "SELECT v.id as v_id, v.user_uuid as v_user_uuid, v.tenant_id as v_tenant_id, " +
            "v.vehicle_number as v_vehicle_number, v.purchase_date as v_purchase_date, " +
            "v.is_verified as v_is_verified, v.validation_source as v_validation_source, " +
            "v.rc_doc_id as v_rc_doc_id, v.vehicle_type as v_vehicle_type, " +
            "v.maker_model as v_maker_model, v.fuel_type as v_fuel_type, " +
            "v.category as v_category, v.status as v_status, " +
            "v.additional_details as v_additional_details, " +
            "v.created_by as v_created_by, v.created_time as v_created_time, " +
            "v.last_modified_by as v_last_modified_by, v.last_modified_time as v_last_modified_time, " +
            "rd.id as rd_id, rd.document_type as rd_document_type, rd.filestore_id as rd_filestore_id, " +
            "rd.document_uid as rd_document_uid, rd.file_name as rd_file_name, rd.is_verified as rd_is_verified " +
            "FROM ug_user_vehicle v " +
            "LEFT JOIN ug_user_document rd ON v.rc_doc_id = rd.id " +
            "WHERE v.user_uuid = :user_uuid";

    /**
     * Default constructor for Spring component lifecycle.
     */
    public UserVehicleQueryBuilder() {
        // Default constructor
    }

    /**
     * Builds dynamic parameterized SQL query for retrieving citizen vehicle ownership declarations.
     *
     * @param active optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return SQL query string
     */
    public String buildVehiclesByUserQuery(Boolean active) {
        StringBuilder query = new StringBuilder(SELECT_VEHICLES_BY_USER_BASE_SQL);
        if (Boolean.TRUE.equals(active)) {
            query.append(" AND v.status = 'ACTIVE'");
        } else if (Boolean.FALSE.equals(active)) {
            query.append(" AND v.status = 'INACTIVE'");
        }
        query.append(" ORDER BY v.created_time DESC");
        return query.toString();
    }

    /**
     * Returns the parameterized SQL insert/upsert query for vehicle declarations.
     *
     * @return SQL insert vehicle statement string
     */
    public String getInsertVehicleQuery() {
        return INSERT_VEHICLE_SQL;
    }

    /**
     * Returns the parameterized SQL audit insertion query for vehicle declarations.
     *
     * @return SQL audit vehicle statement string
     */
    public String getInsertVehicleAuditQuery() {
        return INSERT_VEHICLE_AUDIT_SQL;
    }
}
