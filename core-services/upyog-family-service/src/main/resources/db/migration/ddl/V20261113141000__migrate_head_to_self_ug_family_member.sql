-- ============================================================================
-- Migration: V20261113141000__migrate_head_to_self_ug_family_member.sql
-- Description: Fix for PR Comment #12 (FamilyServiceImpl.java:394):
--              Migrate legacy 'HEAD' relationship to 'SELF' in ug_family_member.
--              In MDMS module 'common-masters', master 'FamilyRelationship',
--              the relationship code for Head of Family was standardized to 'SELF'
--              and 'HEAD' was decommissioned/marked inactive.
--              This data migration updates all existing historical records in
--              ug_family_member where relationship_with_head is 'HEAD' to 'SELF',
--              preventing subsequent update or validation failures.
-- Service: upyog-family-service
-- ============================================================================

UPDATE ug_family_member
SET relationship_with_head = 'SELF'
WHERE UPPER(relationship_with_head) = 'HEAD';
