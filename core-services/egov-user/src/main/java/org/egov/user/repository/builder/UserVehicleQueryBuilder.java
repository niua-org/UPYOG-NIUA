package org.egov.user.repository.builder;

import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.VehicleSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

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

    public static final String SELECT_ACTIVE_VEHICLE_OWNER_BY_NUMBER_SQL =
            "SELECT user_uuid FROM ug_user_vehicle WHERE UPPER(vehicle_number) = UPPER(:vehicle_number) AND status = 'ACTIVE' LIMIT 1";

    public static final String SELECT_VEHICLE_USER_UUID_BY_ID_SQL =
            "SELECT user_uuid FROM ug_user_vehicle WHERE id = :id";

    public static final String SELECT_VEHICLE_BY_ID_SQL =
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
            "WHERE v.id = :id";

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
     * Builds dynamic parameterized SQL query for querying citizen vehicles matching criteria filters.
     *
     * @param criteria search criteria filters
     * @param params   parameter map to populate with query values
     * @return constructed SQL query string
     */
    public String buildSearchQuery(VehicleSearchCriteria criteria, Map<String, Object> params) {
        StringBuilder query = new StringBuilder(
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
                "WHERE 1=1"
        );

        if (criteria == null) {
            query.append(" AND v.status = 'ACTIVE' ORDER BY v.created_time DESC");
            return query.toString();
        }

        String userUuid = criteria.resolveUserUuid();
        if (StringUtils.isNotBlank(userUuid)) {
            query.append(" AND v.user_uuid = :user_uuid");
            params.put("user_uuid", userUuid.trim());
        } else if (!CollectionUtils.isEmpty(criteria.getUserUuids())) {
            query.append(" AND v.user_uuid IN (:user_uuids)");
            params.put("user_uuids", criteria.getUserUuids());
        }

        String vehicleId = criteria.resolveVehicleId();
        if (StringUtils.isNotBlank(vehicleId)) {
            query.append(" AND v.id = :id");
            params.put("id", vehicleId.trim());
        } else if (!CollectionUtils.isEmpty(criteria.getVehicleIds())) {
            query.append(" AND v.id IN (:ids)");
            params.put("ids", criteria.getVehicleIds());
        }

        if (StringUtils.isNotBlank(criteria.getVehicleNumber())) {
            query.append(" AND UPPER(v.vehicle_number) = UPPER(:vehicle_number)");
            params.put("vehicle_number", criteria.getVehicleNumber().trim());
        }

        if (StringUtils.isNotBlank(criteria.getTenantId())) {
            query.append(" AND v.tenant_id = :tenant_id");
            params.put("tenant_id", criteria.getTenantId().trim());
        }

        if (StringUtils.isNotBlank(criteria.getVehicleType())) {
            query.append(" AND UPPER(v.vehicle_type) = UPPER(:vehicle_type)");
            params.put("vehicle_type", criteria.getVehicleType().trim());
        }

        if (StringUtils.isNotBlank(criteria.getFuelType())) {
            query.append(" AND UPPER(v.fuel_type) = UPPER(:fuel_type)");
            params.put("fuel_type", criteria.getFuelType().trim());
        }

        if (StringUtils.isNotBlank(criteria.getCategory())) {
            query.append(" AND UPPER(v.category) = UPPER(:category)");
            params.put("category", criteria.getCategory().trim());
        }

        // Status filter handling
        if (StringUtils.isNotBlank(criteria.getStatus())) {
            String st = criteria.getStatus().trim().toUpperCase();
            if (!UserConstants.STATUS_ALL.equalsIgnoreCase(st)) {
                query.append(" AND v.status = :status");
                params.put("status", st);
            }
        } else if (criteria.getActive() != null) {
            query.append(" AND v.status = :status");
            params.put("status", Boolean.TRUE.equals(criteria.getActive()) ? UserConstants.STATUS_ACTIVE_UPPERCASE : UserConstants.STATUS_INACTIVE_UPPERCASE);
        } else {
            query.append(" AND v.status = 'ACTIVE'");
        }

        query.append(" ORDER BY v.created_time DESC");

        if (criteria.getLimit() != null && criteria.getLimit() > 0) {
            query.append(" LIMIT :limit");
            params.put("limit", criteria.getLimit());
        }
        if (criteria.getOffset() != null && criteria.getOffset() >= 0) {
            query.append(" OFFSET :offset");
            params.put("offset", criteria.getOffset());
        }

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
