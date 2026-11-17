-- ============================================================================
-- Migration: V20261116145500__add_property_id_to_ug_user_rural_property.sql
-- Description: Adds property_id column to ug_user_rural_property for business-readable
--              rural property identification generated via IdGen (aligned with urban upyog_property_id).
-- Service: egov-user
--
-- Legacy ID Format Documentation:
--   - Newly registered rural properties receive standard IdGen identifiers conforming to:
--       [STATE_TENANT]-RP-[YYYY-MM-DD]-[SEQUENCE] (e.g. PG-RP-2026-11-17-000001)
--   - Pre-existing legacy rural property records without a property_id are backfilled as:
--       [STATE_TENANT]-RP-LEGACY-[UUID_PREFIX_8] (e.g. PG-RP-LEGACY-a1b2c3d4)
--     deriving the tenant prefix dynamically from the record's tenant_id (e.g. 'pg.citya' -> 'PG',
--     'ch.cityb' -> 'CH', fallback to 'DEFAULT') to avoid cross-tenant hardcoding while clearly
--     distinguishing pre-migration legacy records from IdGen sequences.
-- ============================================================================

ALTER TABLE ug_user_rural_property ADD COLUMN IF NOT EXISTS property_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_rural_prop_property_id ON ug_user_rural_property(property_id);

-- Backfill existing rural properties with documented tenant-scoped legacy identifier if property_id is null
UPDATE ug_user_rural_property 
SET property_id = COALESCE(NULLIF(UPPER(split_part(tenant_id, '.', 1)), ''), 'DEFAULT') 
                  || '-RP-LEGACY-' 
                  || substring(property_record_id from 1 for 8) 
WHERE property_id IS NULL;
