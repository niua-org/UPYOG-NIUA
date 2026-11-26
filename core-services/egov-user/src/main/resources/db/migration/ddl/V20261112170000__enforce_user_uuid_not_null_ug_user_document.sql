-- -----------------------------------------------------------------------------------------
-- Migration: V20261112170000__enforce_user_uuid_not_null_ug_user_document.sql
-- Description: Follow-up migration for PR Comment #9:
--              1. Re-backfills user_uuid from eg_user for any documents written by older pods
--                 during the rolling deployment window.
--              2. Re-backfills user_id for any documents written without numeric user_id.
--              3. Enforces NOT NULL constraint on user_uuid once backfill verification is complete.
--              4. Enforces document ownership check constraint at database level.
--              5. Establishes bi-directional trigger to auto-sync user_id and user_uuid.
-- -----------------------------------------------------------------------------------------

-- 1. Backfill any rows inserted by older pods during rollout where user_uuid is still NULL
UPDATE ug_user_document d
SET user_uuid = u.uuid
FROM eg_user u
WHERE d.user_id = u.id AND d.user_uuid IS NULL;

-- 2. Backfill user_id for documents created with user_uuid
UPDATE ug_user_document d
SET user_id = u.id
FROM eg_user u
WHERE d.user_uuid = u.uuid AND d.user_id IS NULL;

-- 3. Enforce document ownership integrity at DB level: at least one of user_uuid or user_id must be present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_ug_user_doc_owner'
    ) THEN
        ALTER TABLE ug_user_document
            ADD CONSTRAINT chk_ug_user_doc_owner CHECK (user_uuid IS NOT NULL OR user_id IS NOT NULL);
    END IF;
END $$;

-- 4. Bi-directional trigger to synchronize user_id and user_uuid for rolling deployment safety
CREATE OR REPLACE FUNCTION trg_fn_ug_user_doc_sync_user_uuid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_uuid IS NULL AND NEW.user_id IS NOT NULL THEN
        SELECT uuid INTO NEW.user_uuid FROM eg_user WHERE id = NEW.user_id;
    END IF;
    IF NEW.user_id IS NULL AND NEW.user_uuid IS NOT NULL THEN
        SELECT id INTO NEW.user_id FROM eg_user WHERE uuid = NEW.user_uuid;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ug_user_doc_sync_user_uuid ON ug_user_document;
CREATE TRIGGER trg_ug_user_doc_sync_user_uuid
BEFORE INSERT OR UPDATE ON ug_user_document
FOR EACH ROW
EXECUTE FUNCTION trg_fn_ug_user_doc_sync_user_uuid();

-- 5. Enforce NOT NULL on user_uuid once backfill verification is complete
DO $$
BEGIN
    -- Verify no orphan/unlinked records remain before applying NOT NULL constraint
    IF NOT EXISTS (
        SELECT 1 FROM ug_user_document WHERE user_uuid IS NULL
    ) THEN
        ALTER TABLE ug_user_document ALTER COLUMN user_uuid SET NOT NULL;
    END IF;
END $$;
