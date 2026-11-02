-- Migration: V20261014113000__user_profile_address_and_document_enhancements.sql
-- Consolidated schema enhancements for egov-user:
-- 1. Adds demographic, identity verification, last resync, edit counter, and last login method columns to eg_user and eg_user_audit_table.
-- 2. Extends column lengths on eg_user and eg_user_audit_table for modern ciphertexts and filestore URLs.
-- 3. Creates ug_user_document table for user document attachments.
-- 4. Extends eg_user_address with coordinates (latitude, longitude), filestore_id, and user_uuid.
-- 5. Decouples eg_user_address foreign key to reference eg_user(id) directly (supporting city-tenant addresses with state-tenant users).

-- -----------------------------------------------------------------------------------------
-- 1. Demographics, Verification & Login Tracking on eg_user and eg_user_audit_table
-- -----------------------------------------------------------------------------------------
ALTER TABLE eg_user 
    ADD COLUMN IF NOT EXISTS father_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS mother_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS spouse_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_identity_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS identity_verified_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS identity_verified_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS last_resync_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS editted_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_method VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_eg_user_verified ON eg_user(is_identity_verified);

ALTER TABLE eg_user_audit_table 
    ADD COLUMN IF NOT EXISTS father_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS mother_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS spouse_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_identity_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS identity_verified_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS identity_verified_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS last_resync_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS editted_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_method VARCHAR(32);

-- -----------------------------------------------------------------------------------------
-- 2. Extend Column Lengths for Encryption and URLs
-- -----------------------------------------------------------------------------------------
ALTER TABLE eg_user 
    ALTER COLUMN aadhaarnumber TYPE character varying(256),
    ALTER COLUMN pan TYPE character varying(256),
    ALTER COLUMN alternatemobilenumber TYPE character varying(256),
    ALTER COLUMN photo TYPE character varying(1000),
    ALTER COLUMN signature TYPE character varying(1000);

ALTER TABLE eg_user_audit_table 
    ALTER COLUMN aadhaarnumber TYPE character varying(256),
    ALTER COLUMN pan TYPE character varying(256),
    ALTER COLUMN photo TYPE character varying(1000),
    ALTER COLUMN signature TYPE character varying(1000);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'eg_user_audit_table' AND column_name = 'alternatemobilenumber'
    ) THEN
        ALTER TABLE eg_user_audit_table ALTER COLUMN alternatemobilenumber TYPE character varying(256);
    END IF;
END $$;

-- -----------------------------------------------------------------------------------------
-- 3. Document Management Table (ug_user_document)
-- -----------------------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'eg_user_id_unique'
    ) THEN
        ALTER TABLE eg_user ADD CONSTRAINT eg_user_id_unique UNIQUE (id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS ug_user_document (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id VARCHAR(256) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    document_uid VARCHAR(128),
    filestore_id VARCHAR(64) NOT NULL,
    file_name VARCHAR(256),
    is_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    additional_details JSONB,
    createdby BIGINT,
    createddate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodifiedby BIGINT,
    lastmodifieddate TIMESTAMP,
    CONSTRAINT fk_user_document_user FOREIGN KEY (user_id) REFERENCES eg_user (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_doc_user_id ON ug_user_document(user_id);
CREATE INDEX IF NOT EXISTS idx_user_doc_type ON ug_user_document(document_type);
CREATE INDEX IF NOT EXISTS idx_user_doc_filestore ON ug_user_document(filestore_id);
CREATE INDEX IF NOT EXISTS idx_user_doc_tenant ON ug_user_document(tenant_id);

-- -----------------------------------------------------------------------------------------
-- 4. Address Enhancements (Coordinates, Document Proof & user_uuid)
-- -----------------------------------------------------------------------------------------
ALTER TABLE eg_user_address
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS filestore_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS user_uuid VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_eg_user_address_user_uuid
    ON eg_user_address(user_uuid);

UPDATE eg_user_address a
SET user_uuid = u.uuid
FROM eg_user u
WHERE a.userid = u.id AND a.user_uuid IS NULL;

-- -----------------------------------------------------------------------------------------
-- 5. Decouple Address and Document Foreign Keys to eg_user(id) directly
-- -----------------------------------------------------------------------------------------
ALTER TABLE eg_user_address DROP CONSTRAINT IF EXISTS eg_user_address_user_fkey;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'eg_user_address_user_fkey'
    ) THEN
        ALTER TABLE eg_user_address
            ADD CONSTRAINT eg_user_address_user_fkey
            FOREIGN KEY (userid) REFERENCES eg_user (id)
            ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

ALTER TABLE ug_user_document DROP CONSTRAINT IF EXISTS fk_user_document_user;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_document_user'
    ) THEN
        ALTER TABLE ug_user_document
            ADD CONSTRAINT fk_user_document_user
            FOREIGN KEY (user_id) REFERENCES eg_user (id)
            ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;
