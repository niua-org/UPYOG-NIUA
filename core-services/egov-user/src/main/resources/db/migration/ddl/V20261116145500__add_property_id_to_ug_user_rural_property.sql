-- ============================================================================
-- Migration: V20261116145500__add_property_id_to_ug_user_rural_property.sql
-- Description: Adds property_id column to ug_user_rural_property for business-readable
--              rural property identification generated via IdGen (aligned with urban upyog_property_id).
-- Service: egov-user
-- ============================================================================

ALTER TABLE ug_user_rural_property ADD COLUMN IF NOT EXISTS property_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_rural_prop_property_id ON ug_user_rural_property(property_id);

-- Backfill existing rural properties with generated identifier if any exist without property_id
UPDATE ug_user_rural_property 
SET property_id = 'PG-RP-' || substring(property_record_id from 1 for 8) 
WHERE property_id IS NULL;
