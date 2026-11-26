-- ============================================================================
-- Migration: V20261112151000__create_ug_user_property_3nf_tables.sql
-- Description: Third Normal Form (3NF) tables for Urban and Rural Properties
--              and normalized Property Owners with JSONB organisation metadata.
-- Service: egov-user
-- ============================================================================

-- 1. Base Property Entity
CREATE TABLE IF NOT EXISTS ug_user_property (
    id VARCHAR(64) PRIMARY KEY,
    user_uuid VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    locality_type VARCHAR(32) NOT NULL CHECK (locality_type IN ('URBAN', 'RURAL')),
    ownership_type VARCHAR(32) NOT NULL CHECK (ownership_type IN ('INDIVIDUAL', 'JOINT', 'ORGANISATION')),
    declaration_confirmed BOOLEAN DEFAULT FALSE,
    document_id VARCHAR(64) REFERENCES ug_user_document(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_prop_user_uuid ON ug_user_property(user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_prop_tenant ON ug_user_property(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_prop_locality ON ug_user_property(locality_type);
CREATE INDEX IF NOT EXISTS idx_ug_prop_status ON ug_user_property(status);
CREATE INDEX IF NOT EXISTS idx_ug_prop_doc_id ON ug_user_property(document_id);

-- 2. Urban Property Specialization (1:1 with ug_user_property)
CREATE TABLE IF NOT EXISTS ug_user_urban_property (
    property_record_id VARCHAR(64) PRIMARY KEY REFERENCES ug_user_property(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    upyog_property_id VARCHAR(64) NOT NULL,
    property_tax_amount NUMERIC(12,2) DEFAULT 0.00,
    urban_address VARCHAR(512),
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_urban_prop_upyog_id ON ug_user_urban_property(upyog_property_id);
CREATE INDEX IF NOT EXISTS idx_ug_urban_prop_tenant ON ug_user_urban_property(tenant_id);

-- 3. Rural Property Specialization (1:1 with ug_user_property)
CREATE TABLE IF NOT EXISTS ug_user_rural_property (
    property_record_id VARCHAR(64) PRIMARY KEY REFERENCES ug_user_property(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    gram_panchayat VARCHAR(128) NOT NULL,
    village_name VARCHAR(128) NOT NULL,
    ward_number VARCHAR(64),
    property_address VARCHAR(512),
    property_type VARCHAR(64),
    property_usage VARCHAR(64),
    survey_number VARCHAR(128),
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_rural_prop_gp ON ug_user_rural_property(gram_panchayat);
CREATE INDEX IF NOT EXISTS idx_ug_rural_prop_village ON ug_user_rural_property(village_name);
CREATE INDEX IF NOT EXISTS idx_ug_rural_prop_survey ON ug_user_rural_property(survey_number);

-- 4. Property Owners (Joint Owners and Organisation/Institute)
-- Note: Citizen details (name, mobile, aadhaar) live in eg_user via user_uuid.
-- Organisation details live inside additional_details JSONB.
CREATE TABLE IF NOT EXISTS ug_user_property_owner (
    id VARCHAR(64) PRIMARY KEY,
    property_id VARCHAR(64) NOT NULL REFERENCES ug_user_property(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    owner_type VARCHAR(32) NOT NULL CHECK (owner_type IN ('INDIVIDUAL', 'JOINT_OWNER', 'ORGANISATION')),
    user_uuid VARCHAR(64),
    active BOOLEAN DEFAULT TRUE,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_prop_owner_prop_id ON ug_user_property_owner(property_id);
CREATE INDEX IF NOT EXISTS idx_ug_prop_owner_uuid ON ug_user_property_owner(user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_prop_owner_type ON ug_user_property_owner(owner_type);
CREATE INDEX IF NOT EXISTS idx_ug_prop_owner_org_name 
    ON ug_user_property_owner ((additional_details->>'institutionName')) 
    WHERE owner_type = 'ORGANISATION';

-- 5. Property Base Audit Table
CREATE TABLE IF NOT EXISTS ug_user_property_audit (
    audit_id VARCHAR(64) PRIMARY KEY,
    id VARCHAR(64) NOT NULL,
    user_uuid VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    locality_type VARCHAR(32) NOT NULL,
    ownership_type VARCHAR(32) NOT NULL,
    declaration_confirmed BOOLEAN,
    document_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_prop_audit_id ON ug_user_property_audit(id);
CREATE INDEX IF NOT EXISTS idx_ug_prop_audit_user ON ug_user_property_audit(user_uuid);

-- 6. Property Owner Audit Table
CREATE TABLE IF NOT EXISTS ug_user_property_owner_audit (
    audit_id VARCHAR(64) PRIMARY KEY,
    id VARCHAR(64) NOT NULL,
    property_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    user_uuid VARCHAR(64),
    active BOOLEAN,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_prop_owner_audit_prop ON ug_user_property_owner_audit(property_id);
CREATE INDEX IF NOT EXISTS idx_ug_prop_owner_audit_uuid ON ug_user_property_owner_audit(user_uuid);
