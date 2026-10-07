-- Production already has ug_external_api_message_*. Do not keep state or external_service;
-- external_api_name identifies the integration. Additive columns: endpoint, method.

DROP INDEX IF EXISTS idx_ug_external_api_message_detail_state;
DROP INDEX IF EXISTS idx_ug_external_api_message_detail_external_service;

ALTER TABLE ug_external_api_message_detail DROP COLUMN IF EXISTS state;
ALTER TABLE ug_external_api_message_detail DROP COLUMN IF EXISTS external_service;

ALTER TABLE ug_external_api_message_detail
    ADD COLUMN IF NOT EXISTS endpoint VARCHAR(2048);

ALTER TABLE ug_external_api_message_detail
    ADD COLUMN IF NOT EXISTS method VARCHAR(32);
