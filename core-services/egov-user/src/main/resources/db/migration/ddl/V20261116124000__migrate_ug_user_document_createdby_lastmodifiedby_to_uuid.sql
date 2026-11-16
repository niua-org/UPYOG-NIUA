-- ============================================================================
-- Migration: V20261116124000__migrate_ug_user_document_createdby_lastmodifiedby_to_uuid.sql
-- Description: Migrates ug_user_document createdby and lastmodifiedby audit columns
--              from BIGINT (numeric user ID) to VARCHAR(64) (UUID).
--              Aligns document audit trails with 3NF asset schema (ug_user_property,
--              ug_user_income, ug_user_vehicle).
-- Service: egov-user
-- ============================================================================

-- 1. Convert createdby and lastmodifiedby column types from BIGINT to VARCHAR(64)
ALTER TABLE ug_user_document 
    ALTER COLUMN createdby TYPE VARCHAR(64) USING createdby::text,
    ALTER COLUMN lastmodifiedby TYPE VARCHAR(64) USING lastmodifiedby::text;

-- 2. Populate user UUID from eg_user for existing numeric IDs in createdby and lastmodifiedby
UPDATE ug_user_document d
SET createdby = u.uuid
FROM eg_user u
WHERE d.createdby IS NOT NULL 
  AND d.createdby ~ '^[0-9]+$' 
  AND d.createdby = u.id::text;

UPDATE ug_user_document d
SET lastmodifiedby = u.uuid
FROM eg_user u
WHERE d.lastmodifiedby IS NOT NULL 
  AND d.lastmodifiedby ~ '^[0-9]+$' 
  AND d.lastmodifiedby = u.id::text;

-- 3. Create performance indices on audit columns
CREATE INDEX IF NOT EXISTS idx_ug_user_doc_createdby ON ug_user_document(createdby);
CREATE INDEX IF NOT EXISTS idx_ug_user_doc_lastmodifiedby ON ug_user_document(lastmodifiedby);
