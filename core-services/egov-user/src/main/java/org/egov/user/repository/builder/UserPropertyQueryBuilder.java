package org.egov.user.repository.builder;

import org.springframework.stereotype.Component;

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
            "property_record_id, tenant_id, gram_panchayat, village_name, ward_number, " +
            "property_address, property_type, property_usage, survey_number, " +
            "created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":property_record_id, :tenant_id, :gram_panchayat, :village_name, :ward_number, " +
            ":property_address, :property_type, :property_usage, :survey_number, " +
            ":created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (property_record_id) DO UPDATE SET " +
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
}
