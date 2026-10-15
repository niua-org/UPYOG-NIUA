-- ============================================================
-- Add payment_status and payment_amount columns to ug_grbg_account
-- ============================================================

ALTER TABLE ug_grbg_account ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50);
ALTER TABLE ug_grbg_account ADD COLUMN IF NOT EXISTS payment_amount NUMERIC(12,2);
