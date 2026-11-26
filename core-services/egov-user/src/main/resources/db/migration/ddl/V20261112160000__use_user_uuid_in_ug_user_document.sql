-- -----------------------------------------------------------------------------------------
-- Migration: V20261112160000__use_user_uuid_in_ug_user_document.sql
-- Description: Migrates ug_user_document to use user_uuid instead of user_id.
--              Aligns with 3NF asset schema (ug_user_property, ug_user_income, ug_user_vehicle).
-- -----------------------------------------------------------------------------------------

-- 1. Add user_uuid column to ug_user_document if not present
ALTER TABLE ug_user_document
    ADD COLUMN IF NOT EXISTS user_uuid VARCHAR(64);

-- 2. Populate user_uuid from eg_user for existing document records
UPDATE ug_user_document d
SET user_uuid = u.uuid
FROM eg_user u
WHERE d.user_id = u.id AND d.user_uuid IS NULL;

-- 3. Drop legacy foreign key constraint and relax user_id NOT NULL constraint
ALTER TABLE ug_user_document DROP CONSTRAINT IF EXISTS fk_user_document_user;
ALTER TABLE ug_user_document ALTER COLUMN user_id DROP NOT NULL;

-- 4. Create performance index on user_uuid
CREATE INDEX IF NOT EXISTS idx_ug_user_doc_user_uuid ON ug_user_document(user_uuid);
