-- ============================================================================
-- Migration: V20261120163500__migrate_self_to_hof_ug_family_member.sql
-- Description: Migrate legacy 'SELF' and 'HEAD' relationship codes to 'HOF'
--              in ug_family_member.
--              In MDMS module 'common-masters', master 'FamilyRelationship',
--              the relationship code for Head of Family is standardized to 'HOF'
--              and 'SELF' has been decommissioned/marked inactive.
--              This data migration updates all existing historical records in
--              ug_family_member where relationship_with_head is 'SELF' or 'HEAD'
--              to 'HOF', preventing subsequent update or validation failures.
-- Service: upyog-family-service
-- ============================================================================

UPDATE ug_family_member
SET relationship_with_head = 'HOF'
WHERE UPPER(relationship_with_head) IN ('SELF', 'HEAD');
