DROP INDEX IF EXISTS idx_unique_active_approved_theme;

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_theme
ON ug_theme_config(tenantid, themetype)
WHERE isactive=true;
