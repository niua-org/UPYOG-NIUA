UPDATE eg_vendor 
SET additionaldetails = jsonb_set(COALESCE(additionaldetails, '{}'::jsonb), '{serviceType}', '"FSM"') 
WHERE additionaldetails IS NULL OR additionaldetails->>'serviceType' IS NULL;

UPDATE eg_vendor_auditlog 
SET additionaldetails = jsonb_set(COALESCE(additionaldetails, '{}'::jsonb), '{serviceType}', '"FSM"') 
WHERE additionaldetails IS NULL OR additionaldetails->>'serviceType' IS NULL;
