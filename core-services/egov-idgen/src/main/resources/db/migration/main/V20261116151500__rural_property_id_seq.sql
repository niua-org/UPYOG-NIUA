-- ============================================================================
-- Migration: V20261116151500__rural_property_id_seq.sql
-- Description: Creates sequence for rural property IDs and provides database fallback
--              format rows in id_generator for citizen profile and asset entities.
--
-- Single Source of Truth & Fallback Documentation:
--   1. Primary Source of Truth: In environments where egov-idgen runs with
--      `idformat-from-mdms: true` (e.g. QA, Staging, Production), formats are dynamically
--      resolved from MDMS master `common-masters.IdFormat` (MDMS PR #361).
--   2. Database Fallback: In standalone or local environments where `idformat-from-mdms: false`,
--      IdGenerationService.getIdFormatfromDB() falls back to reading `id_generator` matching
--      (idname, tenantid).
--   3. All 5 new/profile entity ID names are explicitly registered below for the state tenant ('pg').
--   4. Unused 'default' tenant rows are omitted because IdGen specifically queries by
--      the caller's root tenant ('pg') and never selects 'default'.
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS seq_rural_property_id;

-- 1. rural.property.id
INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('rural.property.id', 'pg', 'PG-RP-[cy:yyyy-MM-dd]-[seq_rural_property_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;

-- 2. user.citizen.id
INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('user.citizen.id', 'pg', 'PG-CITIZEN-[cy:yyyy]-[seq_citizen_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;

-- 3. user.document.id
INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('user.document.id', 'pg', 'PG-DOC-[cy:yyyy]-[seq_user_doc_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;

-- 4. family.id
INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('family.id', 'pg', 'PG-FAM-[cy:yyyy]-[seq_family_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;

-- 5. family.member.id
INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('family.member.id', 'pg', 'PG-MEM-[cy:yyyy]-[seq_family_member_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;
