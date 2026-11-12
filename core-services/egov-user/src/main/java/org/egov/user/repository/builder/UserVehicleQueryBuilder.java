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
            "rc_doc_id, vehicle_type, maker_model, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":id, :user_uuid, :tenant_id, :vehicle_number, :purchase_date, :is_verified, :validation_source, " +
            ":rc_doc_id, :vehicle_type, :maker_model, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (id) DO UPDATE SET " +
            "vehicle_number = EXCLUDED.vehicle_number, " +
            "purchase_date = EXCLUDED.purchase_date, " +
            "is_verified = EXCLUDED.is_verified, " +
            "validation_source = EXCLUDED.validation_source, " +
            "rc_doc_id = EXCLUDED.rc_doc_id, " +
            "vehicle_type = EXCLUDED.vehicle_type, " +
            "maker_model = EXCLUDED.maker_model, " +
            "status = EXCLUDED.status, " +
            "additional_details = EXCLUDED.additional_details, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time";

    public static final String INSERT_VEHICLE_AUDIT_SQL =
            "INSERT INTO ug_user_vehicle_audit (" +
            "audit_id, id, user_uuid, tenant_id, vehicle_number, purchase_date, is_verified, validation_source, " +
            "rc_doc_id, vehicle_type, maker_model, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":audit_id, :id, :user_uuid, :tenant_id, :vehicle_number, :purchase_date, :is_verified, :validation_source, " +
            ":rc_doc_id, :vehicle_type, :maker_model, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ")";

    /**
     * Default constructor for Spring component lifecycle.
     */
    public UserVehicleQueryBuilder() {
        // Default constructor
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
