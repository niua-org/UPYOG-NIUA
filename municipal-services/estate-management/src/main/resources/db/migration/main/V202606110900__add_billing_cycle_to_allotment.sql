-- Add billing_cycle column to allotment_details
-- Allowed values: MONTHLY, QUARTERLY
ALTER TABLE ug_em_allotment_details
    ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(20) DEFAULT 'MONTHLY';
