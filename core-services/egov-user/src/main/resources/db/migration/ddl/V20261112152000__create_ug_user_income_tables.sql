-- ============================================================================
-- Migration: V20261112152000__create_ug_user_income_tables.sql
-- Description: Creates schema for Citizen Income and Tax Identifier Registry
-- Service: egov-user
-- ============================================================================

CREATE TABLE IF NOT EXISTS ug_user_income (
    id VARCHAR(64) PRIMARY KEY,
    user_uuid VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    occupation VARCHAR(64) NOT NULL,
    annual_income NUMERIC(14,2) NOT NULL,
    identifier_type VARCHAR(32) NOT NULL CHECK (identifier_type IN ('PAN', 'TAN', 'GSTIN')),
    identifier_value VARCHAR(64),
    is_verified BOOLEAN DEFAULT FALSE,
    verification_source VARCHAR(64),
    verification_date BIGINT,
    otp_verified BOOLEAN DEFAULT FALSE,
    salary_proof_doc_id VARCHAR(64) REFERENCES ug_user_document(id) ON DELETE SET NULL,
    tax_proof_doc_id VARCHAR(64) REFERENCES ug_user_document(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_income_user_uuid ON ug_user_income(user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_income_tenant ON ug_user_income(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_income_identifier ON ug_user_income(identifier_type, identifier_value);
CREATE INDEX IF NOT EXISTS idx_ug_income_status ON ug_user_income(status);
CREATE INDEX IF NOT EXISTS idx_ug_income_salary_doc ON ug_user_income(salary_proof_doc_id);
CREATE INDEX IF NOT EXISTS idx_ug_income_tax_doc ON ug_user_income(tax_proof_doc_id);

-- Income Audit Table
CREATE TABLE IF NOT EXISTS ug_user_income_audit (
    audit_id VARCHAR(64) PRIMARY KEY,
    id VARCHAR(64) NOT NULL,
    user_uuid VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    occupation VARCHAR(64) NOT NULL,
    annual_income NUMERIC(14,2) NOT NULL,
    identifier_type VARCHAR(32) NOT NULL,
    identifier_value VARCHAR(64),
    is_verified BOOLEAN,
    verification_source VARCHAR(64),
    verification_date BIGINT,
    otp_verified BOOLEAN,
    salary_proof_doc_id VARCHAR(64),
    tax_proof_doc_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_income_audit_id ON ug_user_income_audit(id);
CREATE INDEX IF NOT EXISTS idx_ug_income_audit_user ON ug_user_income_audit(user_uuid);
