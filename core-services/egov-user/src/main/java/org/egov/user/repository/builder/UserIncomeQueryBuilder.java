package org.egov.user.repository.builder;

import org.apache.commons.lang3.StringUtils;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * SQL Query Builder component generating persistence queries for citizen income and fiscal declarations
 * targeting {@code ug_user_income} and historical auditing table {@code ug_user_income_audit}.
 * Follows the standard UPYOG QueryBuilder architectural pattern.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class UserIncomeQueryBuilder {

    public static final String INSERT_INCOME_SQL =
            "INSERT INTO ug_user_income (" +
            "id, user_uuid, tenant_id, occupation, annual_income, identifier_type, identifier_value, " +
            "is_verified, verification_source, verification_date, otp_verified, salary_proof_doc_id, " +
            "tax_proof_doc_id, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":id, :user_uuid, :tenant_id, :occupation, :annual_income, :identifier_type, :identifier_value, " +
            ":is_verified, :verification_source, :verification_date, :otp_verified, :salary_proof_doc_id, " +
            ":tax_proof_doc_id, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ") ON CONFLICT (id) DO UPDATE SET " +
            "occupation = EXCLUDED.occupation, " +
            "annual_income = EXCLUDED.annual_income, " +
            "identifier_type = EXCLUDED.identifier_type, " +
            "identifier_value = EXCLUDED.identifier_value, " +
            "is_verified = EXCLUDED.is_verified, " +
            "verification_source = EXCLUDED.verification_source, " +
            "verification_date = EXCLUDED.verification_date, " +
            "otp_verified = EXCLUDED.otp_verified, " +
            "salary_proof_doc_id = EXCLUDED.salary_proof_doc_id, " +
            "tax_proof_doc_id = EXCLUDED.tax_proof_doc_id, " +
            "status = EXCLUDED.status, " +
            "additional_details = EXCLUDED.additional_details, " +
            "last_modified_by = EXCLUDED.last_modified_by, " +
            "last_modified_time = EXCLUDED.last_modified_time " +
            // Security: scope conflict update to owner user_uuid to prevent cross-user income overwrites
            "WHERE ug_user_income.user_uuid = EXCLUDED.user_uuid";

    public static final String INSERT_INCOME_AUDIT_SQL =
            "INSERT INTO ug_user_income_audit (" +
            "audit_id, id, user_uuid, tenant_id, occupation, annual_income, identifier_type, identifier_value, " +
            "is_verified, verification_source, verification_date, otp_verified, salary_proof_doc_id, " +
            "tax_proof_doc_id, status, additional_details, created_by, created_time, last_modified_by, last_modified_time" +
            ") VALUES (" +
            ":audit_id, :id, :user_uuid, :tenant_id, :occupation, :annual_income, :identifier_type, :identifier_value, " +
            ":is_verified, :verification_source, :verification_date, :otp_verified, :salary_proof_doc_id, " +
            ":tax_proof_doc_id, :status, cast(:additional_details as jsonb), :created_by, :created_time, :last_modified_by, :last_modified_time" +
            ")";

    public static final String SELECT_ACTIVE_INCOME_ID_BY_USER_SQL =
            "SELECT id FROM ug_user_income WHERE user_uuid = :user_uuid AND status = 'ACTIVE' LIMIT 1";

    public static final int DEFAULT_SEARCH_LIMIT = 50;
    public static final int MAX_SEARCH_LIMIT = 100;
    public static final int DEFAULT_SEARCH_OFFSET = 0;

    public static final String SELECT_INCOME_USER_UUID_BY_ID_SQL =
            "SELECT user_uuid FROM ug_user_income WHERE id = :id";

    public static final String SELECT_INCOME_BY_ID_SQL =
            "SELECT i.id as i_id, i.user_uuid as i_user_uuid, i.tenant_id as i_tenant_id, " +
            "i.occupation as i_occupation, i.annual_income as i_annual_income, " +
            "i.identifier_type as i_identifier_type, i.identifier_value as i_identifier_value, " +
            "i.is_verified as i_is_verified, i.verification_source as i_verification_source, " +
            "i.verification_date as i_verification_date, i.otp_verified as i_otp_verified, " +
            "i.salary_proof_doc_id as i_salary_proof_doc_id, i.tax_proof_doc_id as i_tax_proof_doc_id, " +
            "i.status as i_status, i.additional_details as i_additional_details, " +
            "i.created_by as i_created_by, i.created_time as i_created_time, " +
            "i.last_modified_by as i_last_modified_by, i.last_modified_time as i_last_modified_time, " +
            "sd.id as sd_id, sd.document_type as sd_document_type, sd.filestore_id as sd_filestore_id, " +
            "sd.document_uid as sd_document_uid, sd.file_name as sd_file_name, sd.is_verified as sd_is_verified, " +
            "td.id as td_id, td.document_type as td_document_type, td.filestore_id as td_filestore_id, " +
            "td.document_uid as td_document_uid, td.file_name as td_file_name, td.is_verified as td_is_verified " +
            "FROM ug_user_income i " +
            "LEFT JOIN ug_user_document sd ON i.salary_proof_doc_id = sd.id " +
            "LEFT JOIN ug_user_document td ON i.tax_proof_doc_id = td.id " +
            "WHERE i.id = :id";

    public static final String SELECT_INCOME_BY_USER_BASE_SQL =
            "SELECT i.id as i_id, i.user_uuid as i_user_uuid, i.tenant_id as i_tenant_id, " +
            "i.occupation as i_occupation, i.annual_income as i_annual_income, " +
            "i.identifier_type as i_identifier_type, i.identifier_value as i_identifier_value, " +
            "i.is_verified as i_is_verified, i.verification_source as i_verification_source, " +
            "i.verification_date as i_verification_date, i.otp_verified as i_otp_verified, " +
            "i.salary_proof_doc_id as i_salary_proof_doc_id, i.tax_proof_doc_id as i_tax_proof_doc_id, " +
            "i.status as i_status, i.additional_details as i_additional_details, " +
            "i.created_by as i_created_by, i.created_time as i_created_time, " +
            "i.last_modified_by as i_last_modified_by, i.last_modified_time as i_last_modified_time, " +
            "sd.id as sd_id, sd.document_type as sd_document_type, sd.filestore_id as sd_filestore_id, " +
            "sd.document_uid as sd_document_uid, sd.file_name as sd_file_name, sd.is_verified as sd_is_verified, " +
            "td.id as td_id, td.document_type as td_document_type, td.filestore_id as td_filestore_id, " +
            "td.document_uid as td_document_uid, td.file_name as td_file_name, td.is_verified as td_is_verified " +
            "FROM ug_user_income i " +
            "LEFT JOIN ug_user_document sd ON i.salary_proof_doc_id = sd.id " +
            "LEFT JOIN ug_user_document td ON i.tax_proof_doc_id = td.id " +
            "WHERE i.user_uuid = :user_uuid";

    /**
     * Default constructor for Spring component lifecycle.
     */
    public UserIncomeQueryBuilder() {
        // Default constructor
    }

    /**
     * Builds dynamic parameterized SQL query for retrieving citizen income declarations.
     *
     * @param active optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return SQL query string
     */
    public String buildIncomeByUserQuery(Boolean active) {
        StringBuilder query = new StringBuilder(SELECT_INCOME_BY_USER_BASE_SQL);
        if (Boolean.TRUE.equals(active)) {
            query.append(" AND i.status = 'ACTIVE'");
        } else if (Boolean.FALSE.equals(active)) {
            query.append(" AND i.status = 'INACTIVE'");
        }
        query.append(" ORDER BY i.created_time DESC LIMIT 1");
        return query.toString();
    }

    /**
     * Builds dynamic parameterized SQL query for batch retrieving citizen income declarations for multiple user UUIDs.
     *
     * @param active optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return SQL query string
     */
    public String buildIncomeByUserUuidsQuery(Boolean active) {
        StringBuilder query = new StringBuilder(
                "SELECT i.id as i_id, i.user_uuid as i_user_uuid, i.tenant_id as i_tenant_id, " +
                "i.occupation as i_occupation, i.annual_income as i_annual_income, " +
                "i.identifier_type as i_identifier_type, i.identifier_value as i_identifier_value, " +
                "i.is_verified as i_is_verified, i.verification_source as i_verification_source, " +
                "i.verification_date as i_verification_date, i.otp_verified as i_otp_verified, " +
                "i.salary_proof_doc_id as i_salary_proof_doc_id, i.tax_proof_doc_id as i_tax_proof_doc_id, " +
                "i.status as i_status, i.additional_details as i_additional_details, " +
                "i.created_by as i_created_by, i.created_time as i_created_time, " +
                "i.last_modified_by as i_last_modified_by, i.last_modified_time as i_last_modified_time, " +
                "sd.id as sd_id, sd.document_type as sd_document_type, sd.filestore_id as sd_filestore_id, " +
                "sd.document_uid as sd_document_uid, sd.file_name as sd_file_name, sd.is_verified as sd_is_verified, " +
                "td.id as td_id, td.document_type as td_document_type, td.filestore_id as td_filestore_id, " +
                "td.document_uid as td_document_uid, td.file_name as td_file_name, td.is_verified as td_is_verified " +
                "FROM ug_user_income i " +
                "LEFT JOIN ug_user_document sd ON i.salary_proof_doc_id = sd.id " +
                "LEFT JOIN ug_user_document td ON i.tax_proof_doc_id = td.id " +
                "WHERE i.user_uuid IN (:user_uuids)"
        );
        if (Boolean.TRUE.equals(active)) {
            query.append(" AND i.status = 'ACTIVE'");
        } else if (Boolean.FALSE.equals(active)) {
            query.append(" AND i.status = 'INACTIVE'");
        }
        query.append(" ORDER BY i.created_time DESC");
        return query.toString();
    }

    /**
     * Builds dynamic parameterized SQL query for searching citizen income declarations based on criteria.
     *
     * @param criteria search criteria filters
     * @param params   parameter map to populate
     * @return SQL query string
     */
    public String buildSearchQuery(IncomeSearchCriteria criteria, Map<String, Object> params) {
        StringBuilder query = new StringBuilder(
                "SELECT i.id as i_id, i.user_uuid as i_user_uuid, i.tenant_id as i_tenant_id, " +
                "i.occupation as i_occupation, i.annual_income as i_annual_income, " +
                "i.identifier_type as i_identifier_type, i.identifier_value as i_identifier_value, " +
                "i.is_verified as i_is_verified, i.verification_source as i_verification_source, " +
                "i.verification_date as i_verification_date, i.otp_verified as i_otp_verified, " +
                "i.salary_proof_doc_id as i_salary_proof_doc_id, i.tax_proof_doc_id as i_tax_proof_doc_id, " +
                "i.status as i_status, i.additional_details as i_additional_details, " +
                "i.created_by as i_created_by, i.created_time as i_created_time, " +
                "i.last_modified_by as i_last_modified_by, i.last_modified_time as i_last_modified_time, " +
                "sd.id as sd_id, sd.document_type as sd_document_type, sd.filestore_id as sd_filestore_id, " +
                "sd.document_uid as sd_document_uid, sd.file_name as sd_file_name, sd.is_verified as sd_is_verified, " +
                "td.id as td_id, td.document_type as td_document_type, td.filestore_id as td_filestore_id, " +
                "td.document_uid as td_document_uid, td.file_name as td_file_name, td.is_verified as td_is_verified " +
                "FROM ug_user_income i " +
                "LEFT JOIN ug_user_document sd ON i.salary_proof_doc_id = sd.id " +
                "LEFT JOIN ug_user_document td ON i.tax_proof_doc_id = td.id " +
                "WHERE 1=1"
        );

        if (criteria != null) {
            if (StringUtils.isNotBlank(criteria.getUserUuid())) {
                query.append(" AND i.user_uuid = :user_uuid");
                params.put("user_uuid", criteria.getUserUuid().trim());
            } else if (!CollectionUtils.isEmpty(criteria.getUserUuids())) {
                query.append(" AND i.user_uuid IN (:user_uuids)");
                params.put("user_uuids", criteria.getUserUuids());
            }

            String effectiveId = criteria.resolveIncomeId();
            if (StringUtils.isNotBlank(effectiveId)) {
                query.append(" AND i.id = :id");
                params.put("id", effectiveId);
            } else if (!CollectionUtils.isEmpty(criteria.getIncomeIds())) {
                query.append(" AND i.id IN (:ids)");
                params.put("ids", criteria.getIncomeIds());
            }

            if (StringUtils.isNotBlank(criteria.getTenantId())) {
                query.append(" AND i.tenant_id = :tenant_id");
                params.put("tenant_id", criteria.getTenantId().trim());
            }

            if (StringUtils.isNotBlank(criteria.getOccupation())) {
                query.append(" AND i.occupation = :occupation");
                params.put("occupation", criteria.getOccupation().trim());
            }

            if (StringUtils.isNotBlank(criteria.getIdentifierType())) {
                query.append(" AND i.identifier_type = :identifier_type");
                params.put("identifier_type", criteria.getIdentifierType().trim());
            }

            if (StringUtils.isNotBlank(criteria.getIdentifierValue())) {
                query.append(" AND i.identifier_value = :identifier_value");
                params.put("identifier_value", criteria.getIdentifierValue().trim());
            }

            if (StringUtils.isNotBlank(criteria.getStatus())) {
                if (!"ALL".equalsIgnoreCase(criteria.getStatus().trim())) {
                    query.append(" AND i.status = :status");
                    params.put("status", criteria.getStatus().trim().toUpperCase());
                }
            } else if (!Boolean.TRUE.equals(criteria.getIncludeDeactivated())) {
                query.append(" AND i.status = 'ACTIVE'");
            }
        } else {
            query.append(" AND i.status = 'ACTIVE'");
        }

        query.append(" ORDER BY i.created_time DESC");

        int limit = DEFAULT_SEARCH_LIMIT;
        if (criteria != null && criteria.getLimit() != null && criteria.getLimit() > 0) {
            limit = Math.min(criteria.getLimit(), MAX_SEARCH_LIMIT);
        }
        int offset = DEFAULT_SEARCH_OFFSET;
        if (criteria != null && criteria.getOffset() != null && criteria.getOffset() >= 0) {
            offset = criteria.getOffset();
        }

        query.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);

        return query.toString();
    }

    /**
     * Returns the parameterized SQL insert/upsert query for citizen income declarations.
     *
     * @return SQL insert income statement string
     */
    public String getInsertIncomeQuery() {
        return INSERT_INCOME_SQL;
    }

    /**
     * Returns the parameterized SQL audit insertion query for citizen income declarations.
     *
     * @return SQL audit income statement string
     */
    public String getInsertIncomeAuditQuery() {
        return INSERT_INCOME_AUDIT_SQL;
    }
}
