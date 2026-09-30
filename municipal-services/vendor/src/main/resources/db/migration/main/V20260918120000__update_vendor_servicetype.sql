UPDATE eg_vendor 
SET additionaldetails = '{"serviceType": "FSM"}'::jsonb 
WHERE additionaldetails IS NULL OR additionaldetails = 'null'::jsonb;

UPDATE eg_vendor 
SET additionaldetails = jsonb_set(additionaldetails, '{serviceType}', '"FSM"') 
WHERE additionaldetails->>'serviceType' IS NULL;

UPDATE eg_vendor_auditlog 
SET additionaldetails = '{"serviceType": "FSM"}'::jsonb 
WHERE additionaldetails IS NULL OR additionaldetails = 'null'::jsonb;

UPDATE eg_vendor_auditlog 
SET additionaldetails = jsonb_set(additionaldetails, '{serviceType}', '"FSM"') 
WHERE additionaldetails->>'serviceType' IS NULL;

UPDATE eg_driver 
SET additionaldetails = '{"serviceType": "FSM"}'::jsonb 
WHERE additionaldetails IS NULL OR additionaldetails = 'null'::jsonb;

UPDATE eg_driver 
SET additionaldetails = jsonb_set(additionaldetails, '{serviceType}', '"FSM"') 
WHERE additionaldetails->>'serviceType' IS NULL;

UPDATE eg_driver_auditlog 
SET additionaldetails = '{"serviceType": "FSM"}'::jsonb 
WHERE additionaldetails IS NULL OR additionaldetails = 'null'::jsonb;

UPDATE eg_driver_auditlog 
SET additionaldetails = jsonb_set(additionaldetails, '{serviceType}', '"FSM"') 
WHERE additionaldetails->>'serviceType' IS NULL;
