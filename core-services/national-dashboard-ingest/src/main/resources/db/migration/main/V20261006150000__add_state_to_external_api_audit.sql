ALTER TABLE ug_external_api_message_detail
    ADD COLUMN IF NOT EXISTS state VARCHAR(256);

CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_detail_state
    ON ug_external_api_message_detail (state);
