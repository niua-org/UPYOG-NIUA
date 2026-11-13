-- ============================================================================
-- Migration: V20261112150000__user_profile_demographics_and_doc_references.sql
-- Description: Adds demographics (is_specially_abled, caste) to eg_user & audit,
--              and adds entity reference columns to ug_user_document.
-- Service: egov-user
-- ============================================================================

-- 1. Demographics on eg_user
ALTER TABLE eg_user 
    ADD COLUMN IF NOT EXISTS is_specially_abled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS caste VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_eg_user_caste ON eg_user(caste);
CREATE INDEX IF NOT EXISTS idx_eg_user_specially_abled ON eg_user(is_specially_abled);

-- 2. Demographics on eg_user_audit_table
ALTER TABLE eg_user_audit_table 
    ADD COLUMN IF NOT EXISTS is_specially_abled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS caste VARCHAR(32);

-- 3. Entity reference columns on ug_user_document for clean multi-entity document isolation
ALTER TABLE ug_user_document
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS reference_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_user_doc_ref ON ug_user_document(reference_type, reference_id);
