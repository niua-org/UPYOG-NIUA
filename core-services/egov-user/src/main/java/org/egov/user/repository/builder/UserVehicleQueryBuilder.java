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
