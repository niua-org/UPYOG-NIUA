CREATE SEQUENCE IF NOT EXISTS seq_rural_property_id;

INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('rural.property.id', 'pg', 'PG-RP-[cy:yyyy-MM-dd]-[seq_rural_property_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;

INSERT INTO id_generator (idname, tenantid, format, sequencenumber) 
VALUES ('rural.property.id', 'default', 'RP-[cy:yyyy-MM-dd]-[seq_rural_property_id]', 1)
ON CONFLICT (idname, tenantid) DO NOTHING;
