-- ============================================================================
-- Migration: V20261112153000__create_ug_user_vehicle_tables.sql
-- Description: Creates schema for Citizen Vehicle Ownership Registry and RC Attachment
-- Service: egov-user
-- ============================================================================

CREATE TABLE IF NOT EXISTS ug_user_vehicle (
    id VARCHAR(64) PRIMARY KEY,
    user_uuid VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vehicle_number VARCHAR(32) NOT NULL,
    purchase_date DATE,
    is_verified BOOLEAN DEFAULT FALSE,
    validation_source VARCHAR(64),
    rc_doc_id VARCHAR(64) REFERENCES ug_user_document(id) ON DELETE SET NULL,
    vehicle_type VARCHAR(64),
    maker_model VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_vehicle_user_uuid ON ug_user_vehicle(user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_vehicle_tenant ON ug_user_vehicle(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_vehicle_number ON ug_user_vehicle(vehicle_number);
CREATE INDEX IF NOT EXISTS idx_ug_vehicle_status ON ug_user_vehicle(status);
CREATE INDEX IF NOT EXISTS idx_ug_vehicle_rc_doc ON ug_user_vehicle(rc_doc_id);

-- Vehicle Audit Table
CREATE TABLE IF NOT EXISTS ug_user_vehicle_audit (
    audit_id VARCHAR(64) PRIMARY KEY,
    id VARCHAR(64) NOT NULL,
    user_uuid VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    vehicle_number VARCHAR(32) NOT NULL,
    purchase_date DATE,
    is_verified BOOLEAN,
    validation_source VARCHAR(64),
    rc_doc_id VARCHAR(64),
    vehicle_type VARCHAR(64),
    maker_model VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT NOT NULL,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_vehicle_audit_id ON ug_user_vehicle_audit(id);
CREATE INDEX IF NOT EXISTS idx_ug_vehicle_audit_user ON ug_user_vehicle_audit(user_uuid);
