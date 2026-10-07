-- Production already has ug_external_api_message_*. Additive columns only.
-- If a previous unreleased script added `state`, rename it to `external_service`.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'ug_external_api_message_detail'
          AND column_name = 'state'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'ug_external_api_message_detail'
          AND column_name = 'external_service'
    ) THEN
        ALTER TABLE ug_external_api_message_detail RENAME COLUMN state TO external_service;
    END IF;
END $$;

ALTER TABLE ug_external_api_message_detail
    ADD COLUMN IF NOT EXISTS external_service VARCHAR(256);

ALTER TABLE ug_external_api_message_detail
    ADD COLUMN IF NOT EXISTS endpoint VARCHAR(2048);

ALTER TABLE ug_external_api_message_detail
    ADD COLUMN IF NOT EXISTS method VARCHAR(32);

DROP INDEX IF EXISTS idx_ug_external_api_message_detail_state;

CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_detail_external_service
    ON ug_external_api_message_detail (external_service);
