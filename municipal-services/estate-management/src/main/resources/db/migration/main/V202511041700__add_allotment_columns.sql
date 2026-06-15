-- Add new columns to ug_em_allotment_details table
ALTER TABLE ug_em_allotment_details
ADD COLUMN asset_reference_no VARCHAR(100),
ADD COLUMN property_type VARCHAR(100),
ADD COLUMN citizen_request_letter VARCHAR(100),
ADD COLUMN allotment_letter VARCHAR(100),
ADD COLUMN signed_deed VARCHAR(100);
