-- ============================================================================
-- Migration: V20261019150000__create_ug_family_tables.sql
-- Description: Unified DDL schema for UPYOG Family Registry & State Family Sync
-- ============================================================================

-- 1. Family Unit Header
CREATE TABLE IF NOT EXISTS ug_family (
    id VARCHAR(64) PRIMARY KEY,
    family_id VARCHAR(64) UNIQUE NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    head_user_uuid VARCHAR(64) NOT NULL,
    category VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

-- 2. Family Members (Many-to-One with ug_family)
CREATE TABLE IF NOT EXISTS ug_family_member (
    id VARCHAR(64) PRIMARY KEY,
    family_id VARCHAR(64) NOT NULL REFERENCES ug_family(family_id),
    tenant_id VARCHAR(64) NOT NULL,
    user_uuid VARCHAR(64) NOT NULL,
    relationship_with_head VARCHAR(64) NOT NULL,
    is_head BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    exit_reason VARCHAR(64),
    exit_date BIGINT,
    consent_token VARCHAR(256),
    consent_date BIGINT,
    effective_from BIGINT,
    effective_to BIGINT,
    additional_details JSONB,
    created_by VARCHAR(64),
    created_time BIGINT,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

-- 3. Family Document Proofs
CREATE TABLE IF NOT EXISTS ug_family_document (
    id VARCHAR(64) PRIMARY KEY,
    family_id VARCHAR(64) NOT NULL REFERENCES ug_family(family_id),
    tenant_id VARCHAR(64) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    filestore_id VARCHAR(64) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(64),
    created_time BIGINT,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT
);

-- 4. Family Lineage / Split & Merge Audit History
CREATE TABLE IF NOT EXISTS ug_family_lineage (
    id VARCHAR(64) PRIMARY KEY,
    parent_family_id VARCHAR(64) NOT NULL,
    child_family_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    reason TEXT,
    created_time BIGINT
);

-- 5. Staging Schema for UC2 State Family Registry Integration
CREATE TABLE IF NOT EXISTS ug_state_family_sync (
    reference_id VARCHAR(64) PRIMARY KEY,
    state_code VARCHAR(32),
    state_family_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    family_name VARCHAR(256),
    status VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    created_time BIGINT,
    last_modified_time BIGINT
);

-- Performance & Lookup Indexes
CREATE INDEX IF NOT EXISTS idx_ug_family_tenant ON ug_family(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_head ON ug_family(head_user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_family_status ON ug_family(status);

CREATE INDEX IF NOT EXISTS idx_ug_family_member_family_id ON ug_family_member(family_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_member_user_uuid ON ug_family_member(user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_family_member_active ON ug_family_member(user_uuid, active);
CREATE INDEX IF NOT EXISTS idx_ug_family_member_lookup ON ug_family_member(tenant_id, family_id, user_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_family_member_status ON ug_family_member(status);
CREATE INDEX IF NOT EXISTS idx_ug_family_member_exit_reason ON ug_family_member(exit_reason);

CREATE INDEX IF NOT EXISTS idx_ug_family_document_family_id ON ug_family_document(family_id);

CREATE INDEX IF NOT EXISTS idx_ug_family_lineage_parent ON ug_family_lineage(parent_family_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_lineage_child ON ug_family_lineage(child_family_id);

CREATE INDEX IF NOT EXISTS idx_ug_state_family_sync_state_family_id ON ug_state_family_sync(state_family_id);
CREATE INDEX IF NOT EXISTS idx_ug_state_family_sync_tenant ON ug_state_family_sync(tenant_id);
