-- ============================================================================
-- Migration: V20261113150000__add_fuel_type_category_to_ug_user_vehicle.sql
-- Description: Adds fuel_type and category attributes to citizen vehicle registry
-- Service: egov-user
-- ============================================================================

ALTER TABLE ug_user_vehicle ADD COLUMN IF NOT EXISTS fuel_type VARCHAR(64);
ALTER TABLE ug_user_vehicle ADD COLUMN IF NOT EXISTS category VARCHAR(64);

ALTER TABLE ug_user_vehicle_audit ADD COLUMN IF NOT EXISTS fuel_type VARCHAR(64);
ALTER TABLE ug_user_vehicle_audit ADD COLUMN IF NOT EXISTS category VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_vehicle_fuel_type ON ug_user_vehicle(fuel_type);
CREATE INDEX IF NOT EXISTS idx_ug_vehicle_category ON ug_user_vehicle(category);
