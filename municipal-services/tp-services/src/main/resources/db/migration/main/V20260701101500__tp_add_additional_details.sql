-- Add additional_details column to store extra information (e.g. digipin)
ALTER TABLE public.upyog_rs_tree_pruning_booking_detail
    ADD COLUMN IF NOT EXISTS additional_details JSONB;

ALTER TABLE public.upyog_rs_tree_pruning_booking_detail_audit
    ADD COLUMN IF NOT EXISTS additional_details JSONB;
