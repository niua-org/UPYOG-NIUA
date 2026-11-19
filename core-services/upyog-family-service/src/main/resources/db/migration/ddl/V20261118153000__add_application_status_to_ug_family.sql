ALTER TABLE ug_family
    ADD COLUMN IF NOT EXISTS application_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS application_no VARCHAR(64),
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS approval_date BIGINT,
    ADD COLUMN IF NOT EXISTS family_certificate_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_family_application_status ON ug_family(application_status);
CREATE INDEX IF NOT EXISTS idx_ug_family_application_no ON ug_family(application_no);

ALTER TABLE ug_family_audit
    ADD COLUMN IF NOT EXISTS application_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS application_no VARCHAR(64),
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS approval_date BIGINT,
    ADD COLUMN IF NOT EXISTS family_certificate_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ug_family_audit_application_status ON ug_family_audit(application_status);
CREATE INDEX IF NOT EXISTS idx_ug_family_audit_application_no ON ug_family_audit(application_no);
