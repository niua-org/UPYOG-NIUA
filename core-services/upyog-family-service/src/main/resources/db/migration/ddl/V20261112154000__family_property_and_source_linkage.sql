-- ============================================================================
-- Migration: V20261112154000__family_property_and_source_linkage.sql
-- Description: Adds family_address_property_id and family_source columns
--              to ug_family and ug_family_audit.
-- Service: upyog-family-service
-- ============================================================================

-- 1. Alter ug_family for property residence link and declaration source
ALTER TABLE ug_family
    ADD COLUMN IF NOT EXISTS family_address_property_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS family_source VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_family_addr_prop_id ON ug_family(family_address_property_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_source ON ug_family(family_source);

-- 2. Alter ug_family_audit table
ALTER TABLE ug_family_audit
    ADD COLUMN IF NOT EXISTS family_address_property_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS family_source VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_family_audit_addr_prop_id ON ug_family_audit(family_address_property_id);
CREATE INDEX IF NOT EXISTS idx_ug_family_audit_source ON ug_family_audit(family_source);
