-- ============================================================================
-- Migration: V20261109130000__add_family_type_to_ug_family.sql
-- Description: Add family_type column to ug_family to distinguish UPYOG_INTERNAL vs STATE_REGISTRY
-- ============================================================================

ALTER TABLE ug_family ADD COLUMN IF NOT EXISTS family_type VARCHAR(64) DEFAULT 'UPYOG_INTERNAL';

CREATE INDEX IF NOT EXISTS idx_ug_family_family_type ON ug_family(family_type);
