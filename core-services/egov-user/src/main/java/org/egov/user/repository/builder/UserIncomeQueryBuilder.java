package org.egov.user.repository.builder;

import org.springframework.stereotype.Component;

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
            "last_modified_time = EXCLUDED.last_modified_time";

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

    /**
     * Default constructor for Spring component lifecycle.
     */
    public UserIncomeQueryBuilder() {
        // Default constructor
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
