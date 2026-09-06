CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_theme_name
ON ug_theme_config(tenantid, themetype, LOWER(themename));
