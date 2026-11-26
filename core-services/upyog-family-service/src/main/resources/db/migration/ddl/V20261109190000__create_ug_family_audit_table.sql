-- ============================================================================
-- Migration: V20261109190000__create_ug_family_audit_table.sql
-- Description: Create audit table for ug_family to record historical state changes
-- ============================================================================

CREATE TABLE IF NOT EXISTS ug_family_audit (
    id VARCHAR(64) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    head_user_uuid VARCHAR(64) NOT NULL,
    category VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    family_type VARCHAR(64),
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_family_audit_family_id ON ug_family_audit(family_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_audit_tenant_id ON ug_family_audit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_audit_head_user_uuid ON ug_family_audit(head_user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_family_audit_last_modified_time ON ug_family_audit(last_modified_time);
