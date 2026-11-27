-- Migration: V20261030115000__add_identity_verified_source.sql
-- Adds identity_verified_source column to track whether a user was verified via AADHAAR, DIGILOCKER, FAMILY_ID, etc.

ALTER TABLE eg_user 
    ADD COLUMN IF NOT EXISTS identity_verified_source VARCHAR(64);

ALTER TABLE eg_user_audit_table 
    ADD COLUMN IF NOT EXISTS identity_verified_source VARCHAR(64);
