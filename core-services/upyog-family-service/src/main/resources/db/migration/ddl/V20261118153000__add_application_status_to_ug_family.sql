ALTER TABLE ug_family
    ADD COLUMN IF NOT EXISTS application_status VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_family_application_status ON ug_family(application_status);

ALTER TABLE ug_family_audit
    ADD COLUMN IF NOT EXISTS application_status VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_family_audit_application_status ON ug_family_audit(application_status);
