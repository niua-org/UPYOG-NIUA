package org.egov.user.repository.builder;

import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SQL Query Builder component generating persistence queries for citizen property declarations across
 * 3NF tables: {@code ug_user_property}, {@code ug_user_urban_property},
 * {@code ug_user_rural_property}, {@code ug_user_property_owner}, and their historical audit tables.
 * Follows the standard UPYOG QueryBuilder architectural pattern.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class UserPropertyQueryBuilder {

    public static final String INSERT_PROPERTY_SQL =
            "INSERT INTO ug_user_property (" +
            "id, user_uuid, tenant_id, locality_type, ownership_type, declaration_confirmed, " +
            "document_id, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":id, :user_uuid, :tenant_id, :locality_type, :ownership_type, :declaration_confirmed, " +
            ":document_id, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (id) DO UPDATE SET " +
            "locality_type = EXCLUDED.locality_type, " +
            "ownership_type = EXCLUDED.ownership_type, " +
            "declaration_confirmed = EXCLUDED.declaration_confirmed, " +
            "document_id = EXCLUDED.document_id, " +
            "status = EXCLUDED.status, " +
            "additional_details = EXCLUDED.additional_details, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time " +
            // Security: scope conflict update to owner user_uuid to prevent cross-user property overwrites
            "WHERE ug_user_property.user_uuid = EXCLUDED.user_uuid";

    public static final String INSERT_PROPERTY_AUDIT_SQL =
            "INSERT INTO ug_user_property_audit (" +
            "audit_id, id, user_uuid, tenant_id, locality_type, ownership_type, declaration_confirmed, " +
            "document_id, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":audit_id, :id, :user_uuid, :tenant_id, :locality_type, :ownership_type, :declaration_confirmed, " +
            ":document_id, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ")";

    public static final String INSERT_URBAN_PROPERTY_SQL =
            "INSERT INTO ug_user_urban_property (" +
            "property_record_id, tenant_id, upyog_property_id, property_tax_amount, urban_address, " +
            "created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":property_record_id, :tenant_id, :upyog_property_id, :property_tax_amount, :urban_address, " +
            ":created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (property_record_id) DO UPDATE SET " +
            "upyog_property_id = EXCLUDED.upyog_property_id, " +
            "property_tax_amount = EXCLUDED.property_tax_amount, " +
            "urban_address = EXCLUDED.urban_address, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time";

    public static final String INSERT_RURAL_PROPERTY_SQL =
            "INSERT INTO ug_user_rural_property (" +
            "property_record_id, tenant_id, property_id, gram_panchayat, village_name, ward_number, " +
            "property_address, property_type, property_usage, survey_number, " +
            "created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":property_record_id, :tenant_id, :property_id, :gram_panchayat, :village_name, :ward_number, " +
            ":property_address, :property_type, :property_usage, :survey_number, " +
            ":created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (property_record_id) DO UPDATE SET " +
            "property_id = COALESCE(EXCLUDED.property_id, ug_user_rural_property.property_id), " +
            "gram_panchayat = EXCLUDED.gram_panchayat, " +
            "village_name = EXCLUDED.village_name, " +
            "ward_number = EXCLUDED.ward_number, " +
            "property_address = EXCLUDED.property_address, " +
            "property_type = EXCLUDED.property_type, " +
            "property_usage = EXCLUDED.property_usage, " +
            "survey_number = EXCLUDED.survey_number, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time";

    public static final String INSERT_PROPERTY_OWNER_SQL =
            "INSERT INTO ug_user_property_owner (" +
            "id, property_id, tenant_id, owner_type, user_uuid, active, additional_details, " +
            "created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":id, :property_id, :tenant_id, :owner_type, :user_uuid, :active, cast(:additional_details as jsonb), " +
            ":created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (id) DO UPDATE SET " +
            "owner_type = EXCLUDED.owner_type, " +
            "active = EXCLUDED.active, " +
            "additional_details = EXCLUDED.additional_details, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time " +
            // Security: scope conflict update to matching property and owner user_uuid to prevent reassigning ownership
            "WHERE ug_user_property_owner.property_id = EXCLUDED.property_id AND ug_user_property_owner.user_uuid = EXCLUDED.user_uuid";

    public static final String INSERT_PROPERTY_OWNER_AUDIT_SQL =
            "INSERT INTO ug_user_property_owner_audit (" +
            "audit_id, id, property_id, tenant_id, owner_type, user_uuid, active, additional_details, " +
            "created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":audit_id, :id, :property_id, :tenant_id, :owner_type, :user_uuid, :active, cast(:additional_details as jsonb), " +
            ":created_by, :created_time, :last_modified_by, :last_modified_time" +
            ")";

    public static final String SELECT_PROPERTY_OWNER_ID_BY_PROPERTY_AND_USER_SQL =
            "SELECT id FROM ug_user_property_owner " +
            "WHERE property_id = :property_id AND user_uuid = :user_uuid AND active = true LIMIT 1";

    public static final String SELECT_PROPERTY_USER_UUID_BY_ID_SQL =
            "SELECT user_uuid FROM ug_user_property WHERE id = :id";

    public static final String SELECT_URBAN_PROPERTY_RECORD_ID_SQL =
            "SELECT p.id FROM ug_user_property p JOIN ug_user_urban_property u ON p.id = u.property_record_id " +
            "WHERE p.user_uuid = :user_uuid AND u.upyog_property_id = :upyog_property_id AND p.status = 'ACTIVE' LIMIT 1";

    public static final String SELECT_RURAL_PROPERTY_RECORD_ID_SQL =
            "SELECT p.id FROM ug_user_property p JOIN ug_user_rural_property r ON p.id = r.property_record_id " +
            "WHERE p.user_uuid = :user_uuid AND r.survey_number = :survey_number AND p.status = 'ACTIVE' LIMIT 1";

    public static final String SELECT_RURAL_PROPERTY_RECORD_ID_BY_PROPERTY_ID_SQL =
            "SELECT p.id FROM ug_user_property p JOIN ug_user_rural_property r ON p.id = r.property_record_id " +
            "WHERE p.user_uuid = :user_uuid AND r.property_id = :property_id AND p.status = 'ACTIVE' LIMIT 1";

    public static final String SELECT_RURAL_PROPERTY_BASE_SQL =
            "SELECT p.id as p_id, p.user_uuid as p_user_uuid, p.tenant_id as p_tenant_id, " +
            "p.locality_type as p_locality_type, p.ownership_type as p_ownership_type, " +
            "p.declaration_confirmed as p_declaration_confirmed, p.document_id as p_document_id, " +
            "p.status as p_status, p.additional_details as p_additional_details, " +
            "p.created_by as p_created_by, p.created_time as p_created_time, " +
            "p.last_modified_by as p_last_modified_by, p.last_modified_time as p_last_modified_time, " +
            "r.property_record_id as r_property_record_id, r.tenant_id as r_tenant_id, r.property_id as r_property_id, " +
            "r.gram_panchayat as r_gram_panchayat, r.village_name as r_village_name, " +
            "r.ward_number as r_ward_number, r.property_address as r_property_address, " +
            "r.property_type as r_property_type, r.property_usage as r_property_usage, " +
            "r.survey_number as r_survey_number, " +
            "r.created_by as r_created_by, r.created_time as r_created_time, " +
            "r.last_modified_by as r_last_modified_by, r.last_modified_time as r_last_modified_time, " +
            "d.id as d_id, d.document_type as d_document_type, d.filestore_id as d_filestore_id, " +
            "d.document_uid as d_document_uid, d.file_name as d_file_name, d.is_verified as d_is_verified " +
            "FROM ug_user_property p " +
            "JOIN ug_user_rural_property r ON p.id = r.property_record_id " +
            "LEFT JOIN ug_user_document d ON p.document_id = d.id " +
            "WHERE p.locality_type = 'RURAL'";

    public static final String SELECT_PROPERTIES_BY_USER_BASE_SQL =
            "SELECT p.id as p_id, p.user_uuid as p_user_uuid, p.tenant_id as p_tenant_id, " +
            "p.locality_type as p_locality_type, p.ownership_type as p_ownership_type, " +
            "p.declaration_confirmed as p_declaration_confirmed, p.document_id as p_document_id, " +
            "p.status as p_status, p.additional_details as p_additional_details, " +
            "p.created_by as p_created_by, p.created_time as p_created_time, " +
            "p.last_modified_by as p_last_modified_by, p.last_modified_time as p_last_modified_time, " +
            "u.property_record_id as u_property_record_id, u.tenant_id as u_tenant_id, " +
            "u.upyog_property_id as u_upyog_property_id, u.property_tax_amount as u_property_tax_amount, " +
            "u.urban_address as u_urban_address, u.created_by as u_created_by, " +
            "u.created_time as u_created_time, u.last_modified_by as u_last_modified_by, " +
            "u.last_modified_time as u_last_modified_time, " +
            "r.property_record_id as r_property_record_id, r.tenant_id as r_tenant_id, r.property_id as r_property_id, " +
            "r.gram_panchayat as r_gram_panchayat, r.village_name as r_village_name, " +
            "r.ward_number as r_ward_number, r.property_address as r_property_address, " +
            "r.property_type as r_property_type, r.property_usage as r_property_usage, " +
            "r.survey_number as r_survey_number, r.created_by as r_created_by, " +
            "r.created_time as r_created_time, r.last_modified_by as r_last_modified_by, " +
            "r.last_modified_time as r_last_modified_time, " +
            "d.id as d_id, d.document_type as d_document_type, d.filestore_id as d_filestore_id, " +
            "d.document_uid as d_document_uid, d.file_name as d_file_name, d.is_verified as d_is_verified " +
            "FROM ug_user_property p " +
            "LEFT JOIN ug_user_urban_property u ON p.id = u.property_record_id " +
            "LEFT JOIN ug_user_rural_property r ON p.id = r.property_record_id " +
            "LEFT JOIN ug_user_document d ON p.document_id = d.id " +
            "WHERE p.user_uuid = :user_uuid";

    public static final String SELECT_PROPERTY_OWNERS_BY_PROPERTY_IDS_SQL =
            "SELECT id, property_id, tenant_id, owner_type, user_uuid, active, additional_details, " +
            "created_by, created_time, last_modified_by, last_modified_time " +
            "FROM ug_user_property_owner WHERE property_id IN (:property_ids) AND active = true";

    public static final String UPDATE_PROPERTY_STATUS_SQL =
            "UPDATE ug_user_property SET status = :status, last_modified_by = :last_modified_by, " +
            "last_modified_time = :last_modified_time WHERE id = :id AND user_uuid = :user_uuid";

    public static final String DEACTIVATE_PROPERTY_OWNERS_SQL =
            "UPDATE ug_user_property_owner SET active = false, last_modified_by = :last_modified_by, " +
            "last_modified_time = :last_modified_time WHERE property_id = :property_id";

    /**
     * Default constructor for Spring component lifecycle.
     */
    public UserPropertyQueryBuilder() {
        // Default constructor
    }

    /**
     * Returns the parameterized SQL insert/upsert query for property declarations.
     *
     * @return SQL insert statement string
     */
    public String getInsertPropertyQuery() {
        return INSERT_PROPERTY_SQL;
    }

    /**
     * Returns the parameterized SQL audit insertion query for property declarations.
     *
     * @return SQL audit statement string
     */
    public String getInsertPropertyAuditQuery() {
        return INSERT_PROPERTY_AUDIT_SQL;
    }

    /**
     * Returns the parameterized SQL insert/upsert query for urban property child details.
     *
     * @return SQL insert urban property statement string
     */
    public String getInsertUrbanPropertyQuery() {
        return INSERT_URBAN_PROPERTY_SQL;
    }

    /**
     * Returns the parameterized SQL insert/upsert query for rural property child details.
     *
     * @return SQL insert rural property statement string
     */
    public String getInsertRuralPropertyQuery() {
        return INSERT_RURAL_PROPERTY_SQL;
    }

    /**
     * Returns the parameterized SQL insert/upsert query for property co-owners.
     *
     * @return SQL insert property owner statement string
     */
    public String getInsertPropertyOwnerQuery() {
        return INSERT_PROPERTY_OWNER_SQL;
    }

    /**
     * Returns the parameterized SQL audit insertion query for property co-owners.
     *
     * @return SQL audit property owner statement string
     */
    public String getInsertPropertyOwnerAuditQuery() {
        return INSERT_PROPERTY_OWNER_AUDIT_SQL;
    }

    /**
     * Builds dynamic parameterized SQL query for querying rural properties.
     *
     * @param criteria search filter criteria
     * @param params   parameter map to populate with named bind variables
     * @return fully constructed SQL query string
     */
    public String buildRuralPropertySearchQuery(RuralPropertySearchCriteria criteria, Map<String, Object> params) {
        StringBuilder query = new StringBuilder(SELECT_RURAL_PROPERTY_BASE_SQL);

        if (criteria != null) {
            if (StringUtils.isNotBlank(criteria.getUserUuid())) {
                query.append(" AND p.user_uuid = :user_uuid");
                params.put("user_uuid", criteria.getUserUuid().trim());
            }

            if (criteria.getUserUuids() != null && !criteria.getUserUuids().isEmpty()) {
                query.append(" AND p.user_uuid IN (:user_uuids)");
                params.put("user_uuids", criteria.getUserUuids());
            }

            if (StringUtils.isNotBlank(criteria.getPropertyId())) {
                query.append(" AND (p.id = :property_id OR r.property_id = :property_id)");
                params.put("property_id", criteria.getPropertyId().trim());
            }

            if (criteria.getPropertyIds() != null && !criteria.getPropertyIds().isEmpty()) {
                query.append(" AND (p.id IN (:property_ids) OR r.property_id IN (:property_ids))");
                params.put("property_ids", criteria.getPropertyIds());
            }

            if (StringUtils.isNotBlank(criteria.getTenantId())) {
                query.append(" AND p.tenant_id = :tenant_id");
                params.put("tenant_id", criteria.getTenantId().trim());
            }

            if (StringUtils.isNotBlank(criteria.getGramPanchayat())) {
                query.append(" AND LOWER(r.gram_panchayat) = LOWER(:gram_panchayat)");
                params.put("gram_panchayat", criteria.getGramPanchayat().trim());
            }

            if (StringUtils.isNotBlank(criteria.getVillageName())) {
                query.append(" AND LOWER(r.village_name) = LOWER(:village_name)");
                params.put("village_name", criteria.getVillageName().trim());
            }

            if (StringUtils.isNotBlank(criteria.getSurveyNumber())) {
                query.append(" AND r.survey_number = :survey_number");
                params.put("survey_number", criteria.getSurveyNumber().trim());
            }

            boolean allStatus = Boolean.TRUE.equals(criteria.getIncludeDeactivated())
                    || UserConstants.STATUS_ALL.equalsIgnoreCase(criteria.getStatus());

            if (!allStatus) {
                if (StringUtils.isNotBlank(criteria.getStatus())) {
                    query.append(" AND p.status = :status");
                    params.put("status", criteria.getStatus().trim().toUpperCase());
                } else {
                    query.append(" AND p.status = :status");
                    params.put("status", UserConstants.STATUS_ACTIVE_UPPERCASE);
                }
            }
        } else {
            query.append(" AND p.status = 'ACTIVE'");
        }

        query.append(" ORDER BY p.created_time DESC");
        return query.toString();
    }

    /**
     * Returns the parameterized SQL query for retrieving co-owners of given property IDs.
     *
     * @return SQL query string
     */
    public String getSelectPropertyOwnersByPropertyIdsQuery() {
        return SELECT_PROPERTY_OWNERS_BY_PROPERTY_IDS_SQL;
    }

    /**
     * Returns the parameterized SQL query for updating property lifecycle status.
     *
     * @return SQL query string
     */
    public String getUpdatePropertyStatusQuery() {
        return UPDATE_PROPERTY_STATUS_SQL;
    }

    /**
     * Builds dynamic parameterized SQL query for retrieving citizen property assets across urban and rural domains.
     *
     * @param active optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @param params parameter map to populate with named bind variables
     * @return fully constructed SQL query string
     */
    public String buildPropertiesByUserQuery(Boolean active, Map<String, Object> params) {
        StringBuilder query = new StringBuilder(SELECT_PROPERTIES_BY_USER_BASE_SQL);
        if (Boolean.TRUE.equals(active)) {
            query.append(" AND p.status = 'ACTIVE'");
        } else if (Boolean.FALSE.equals(active)) {
            query.append(" AND p.status = 'INACTIVE'");
        }
        query.append(" ORDER BY p.created_time DESC");
        return query.toString();
    }

    /**
     * Builds dynamic parameterized SQL query for batch retrieving citizen property assets across multiple citizen user UUIDs.
     *
     * @param active optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return fully constructed SQL query string
     */
    public String buildPropertiesByUserUuidsQuery(Boolean active) {
        StringBuilder query = new StringBuilder(
                "SELECT p.id as p_id, p.user_uuid as p_user_uuid, p.tenant_id as p_tenant_id, " +
                "p.locality_type as p_locality_type, p.ownership_type as p_ownership_type, " +
                "p.declaration_confirmed as p_declaration_confirmed, p.document_id as p_document_id, " +
                "p.status as p_status, p.additional_details as p_additional_details, " +
                "p.created_by as p_created_by, p.created_time as p_created_time, " +
                "p.last_modified_by as p_last_modified_by, p.last_modified_time as p_last_modified_time, " +
                "u.property_record_id as u_property_record_id, u.tenant_id as u_tenant_id, " +
                "u.upyog_property_id as u_upyog_property_id, u.property_tax_amount as u_property_tax_amount, " +
                "u.urban_address as u_urban_address, u.created_by as u_created_by, " +
                "u.created_time as u_created_time, u.last_modified_by as u_last_modified_by, " +
                "u.last_modified_time as u_last_modified_time, " +
                "r.property_record_id as r_property_record_id, r.tenant_id as r_tenant_id, r.property_id as r_property_id, " +
                "r.gram_panchayat as r_gram_panchayat, r.village_name as r_village_name, " +
                "r.ward_number as r_ward_number, r.property_address as r_property_address, " +
                "r.property_type as r_property_type, r.property_usage as r_property_usage, " +
                "r.survey_number as r_survey_number, r.created_by as r_created_by, " +
                "r.created_time as r_created_time, r.last_modified_by as r_last_modified_by, " +
                "r.last_modified_time as r_last_modified_time, " +
                "d.id as d_id, d.document_type as d_document_type, d.filestore_id as d_filestore_id, " +
                "d.document_uid as d_document_uid, d.file_name as d_file_name, d.is_verified as d_is_verified " +
                "FROM ug_user_property p " +
                "LEFT JOIN ug_user_urban_property u ON p.id = u.property_record_id " +
                "LEFT JOIN ug_user_rural_property r ON p.id = r.property_record_id " +
                "LEFT JOIN ug_user_document d ON p.document_id = d.id " +
                "WHERE p.user_uuid IN (:user_uuids)"
        );
        if (Boolean.TRUE.equals(active)) {
            query.append(" AND p.status = 'ACTIVE'");
        } else if (Boolean.FALSE.equals(active)) {
            query.append(" AND p.status = 'INACTIVE'");
        }
        query.append(" ORDER BY p.created_time DESC");
        return query.toString();
    }

    /**
     * Returns the parameterized SQL query for deactivating property owners.
     *
     * @return SQL query string
     */
    public String getDeactivatePropertyOwnersQuery() {
        return DEACTIVATE_PROPERTY_OWNERS_SQL;
    }
}
