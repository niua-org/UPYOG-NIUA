-- =============================================================================
-- Migration: V20260907150000__alter_ingestion_module_detail_drop_unused_columns
-- Description: Drops legacy status and scheduling columns from ingestion_module_detail.
-- =============================================================================

ALTER TABLE ingestion_module_detail
    DROP COLUMN IF EXISTS is_legacy_data_ingested,
    DROP COLUMN IF EXISTS last_ingested_date,
    DROP COLUMN IF EXISTS ulb_name,
    DROP COLUMN IF EXISTS schedule_cron;
