CREATE SEQUENCE IF NOT EXISTS seq_chb_booking_id;
CREATE TABLE IF NOT EXISTS public.eg_chb_booking_detail (
  booking_id character varying(64) NOT NULL, 
  booking_no character varying(64), 
  payment_date bigint, 
  application_date bigint NOT NULL, 
  tenant_id character varying(64) NOT NULL, 
  community_hall_code character varying(64) NOT NULL, 
  booking_status character varying(30) NOT NULL, 
  special_category character varying(60) NOT NULL, 
  purpose character varying(60) NOT NULL, 
  purpose_description character varying(100) NOT NULL, 
  receipt_no character varying(64), 
  createdby character varying(64) NOT NULL, 
  createdtime bigint NOT NULL, 
  lastmodifiedby character varying(64), 
  lastmodifiedtime bigint, 
  permission_letter_filestore_id character varying(64), 
  payment_receipt_filestore_id character varying(64), 
  CONSTRAINT eg_chb_booking_detail_pk PRIMARY KEY (booking_id), 
  CONSTRAINT eg_chb_booking_detail_booking_no_key UNIQUE (booking_no)
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_booking_detail_booking_no ON public.eg_chb_booking_detail USING btree (booking_no ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_eg_chb_booking_detail_community_hall_code ON public.eg_chb_booking_detail USING btree (
  community_hall_code ASC NULLS LAST
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_booking_detail_createdby ON public.eg_chb_booking_detail USING btree (createdby ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_eg_chb_booking_detail_tenant_id ON public.eg_chb_booking_detail USING btree (tenant_id ASC NULLS LAST);
-------
CREATE TABLE IF NOT EXISTS public.eg_chb_applicant_detail (
  applicant_detail_id character varying(64) NOT NULL, 
  booking_id character varying(64) NOT NULL, 
  applicant_name character varying(300) NOT NULL, 
  applicant_email_id character varying(300) NOT NULL, 
  applicant_mobile_no character varying(150) NOT NULL, 
  applicant_alternate_mobile_no character varying(150), 
  account_no character varying(200) NOT NULL, 
  ifsc_code character varying(200) NOT NULL, 
  bank_name character varying(300) NOT NULL, 
  bank_branch_name character varying(300) NOT NULL, 
  account_holder_name character varying(300) NOT NULL, 
  refund_status character varying(30), 
  refund_type character varying(15), 
  createdby character varying(64), 
  lastmodifiedby character varying(64), 
  createdtime bigint, 
  lastmodifiedtime bigint, 
  CONSTRAINT eg_chb_applicant_detail_id_pk PRIMARY KEY (applicant_detail_id), 
  CONSTRAINT eg_chb_bank_detail_booking_id_fk FOREIGN KEY (booking_id) REFERENCES public.eg_chb_booking_detail (booking_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_applicant_detail_applicant_mobile_no ON public.eg_chb_applicant_detail USING btree (
  applicant_mobile_no ASC NULLS LAST
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_applicant_detail_booking_id ON public.eg_chb_applicant_detail USING btree (booking_id ASC NULLS LAST);
-------

CREATE TABLE IF NOT EXISTS public.eg_chb_address_detail (
  address_id character varying(64) NOT NULL, 
  applicant_detail_id character varying(64) NOT NULL, 
  door_no character varying(100), 
  house_no character varying(100), 
  street_name character varying(150), 
  address_line_1 character varying(150), 
  landmark character varying(150), 
  city character varying(100) NOT NULL, 
  city_code character varying(10) NOT NULL, 
  locality character varying(100) NOT NULL, 
  locality_code character varying(20) NOT NULL, 
  pincode character varying(12) NOT NULL, 
  CONSTRAINT eg_chb_address_detail_id_pk PRIMARY KEY (address_id), 
  CONSTRAINT eg_chb_address_applicant_detail_id_fk FOREIGN KEY (applicant_detail_id) REFERENCES public.eg_chb_applicant_detail (applicant_detail_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_address_detail_applicant_detail_id ON public.eg_chb_address_detail USING btree (
  applicant_detail_id ASC NULLS LAST
);
------


CREATE TABLE IF NOT EXISTS public.eg_chb_booking_detail_audit (
  booking_id character varying(64) NOT NULL, 
  booking_no character varying(64), 
  payment_date bigint, 
  application_date bigint NOT NULL, 
  tenant_id character varying(64) NOT NULL, 
  community_hall_code character varying(64) NOT NULL, 
  booking_status character varying(30) NOT NULL, 
  special_category character varying(60) NOT NULL, 
  purpose character varying(60) NOT NULL, 
  purpose_description character varying(100) NOT NULL, 
  receipt_no character varying(64), 
  createdby character varying(64) NOT NULL, 
  createdtime bigint NOT NULL, 
  lastmodifiedby character varying(64), 
  lastmodifiedtime bigint, 
  permission_letter_filestore_id character varying(64), 
  payment_receipt_filestore_id character varying(64)
);
------
CREATE TABLE IF NOT EXISTS public.eg_chb_booking_detail_init (
  booking_id character varying(64) NOT NULL, 
  tenant_id character varying(10) NOT NULL, 
  community_hall_id character varying(64) NOT NULL, 
  booking_status character varying(30) NOT NULL, 
  booking_details jsonb, 
  createdby character varying(64) NOT NULL, 
  createdtime bigint NOT NULL, 
  lastmodifiedby character varying(64), 
  lastmodifiedtime bigint, 
  CONSTRAINT eg_chb_booking_details_init PRIMARY KEY (booking_id)
);
-------
CREATE TABLE IF NOT EXISTS public.eg_chb_document_detail (
  document_detail_id character varying(64) NOT NULL, 
  booking_id character varying(64) NOT NULL, 
  document_type character varying(64), 
  filestore_id character varying(64) NOT NULL, 
  createdby character varying(64), 
  lastmodifiedby character varying(64), 
  createdtime bigint, 
  lastmodifiedtime bigint, 
  CONSTRAINT eg_chb_document_detail_id_pk PRIMARY KEY (document_detail_id), 
  CONSTRAINT eg_chb_document_detail_booking_id_fk FOREIGN KEY (booking_id) REFERENCES public.eg_chb_booking_detail (booking_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_document_detail_booking_id ON public.eg_chb_document_detail USING btree (booking_id ASC NULLS LAST);
---------
CREATE SEQUENCE IF NOT EXISTS public.eg_chb_payment_timer_timer_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    NO CYCLE;

CREATE TABLE IF NOT EXISTS public.eg_chb_payment_timer (
  booking_id character varying(64) NOT NULL, 
  createdby character varying(64) NOT NULL, 
  createdtime bigint NOT NULL, 
  status character varying(64) NOT NULL, 
  timer_id integer NOT NULL DEFAULT nextval('public.eg_chb_payment_timer_timer_id_seq'::regclass), 
  booking_no character varying(64), 
  community_hall_code character varying(64), 
  hall_code character varying(64), 
  lastmodifiedby character varying(64), 
  lastmodifiedtime bigint, 
  booking_date date, 
  tenant_id character varying(64), 
  CONSTRAINT eg_chb_payment_timer_pkey PRIMARY KEY (timer_id)
);

-------
CREATE TABLE IF NOT EXISTS public.eg_chb_slot_detail (
  slot_id character varying(64) NOT NULL, 
  booking_id character varying(64) NOT NULL, 
  hall_code character varying(64) NOT NULL, 
  capacity character varying(20) NOT NULL, 
  booking_date date NOT NULL, 
  booking_from_time time without time zone NOT NULL, 
  booking_to_time time without time zone NOT NULL, 
  status character varying(30) NOT NULL, 
  createdby character varying(64) NOT NULL, 
  createdtime bigint NOT NULL, 
  lastmodifiedby character varying(64), 
  lastmodifiedtime bigint, 
  CONSTRAINT eg_chb_slot_detail_slot_id_pk PRIMARY KEY (slot_id), 
  CONSTRAINT eg_chb_slot_detail_booking_id_fk FOREIGN KEY (booking_id) REFERENCES public.eg_chb_booking_detail (booking_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_eg_chb_slot_detail_booking_date ON public.eg_chb_slot_detail USING btree (booking_date ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_eg_chb_slot_detail_booking_id ON public.eg_chb_slot_detail USING btree (booking_id ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_eg_chb_slot_detail_hall_code ON public.eg_chb_slot_detail USING btree (hall_code ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_eg_chb_slot_detail_status ON public.eg_chb_slot_detail USING btree (status ASC NULLS LAST);
-------
CREATE TABLE IF NOT EXISTS public.eg_chb_slot_detail_audit (
  slot_id character varying(64) NOT NULL, 
  booking_id character varying(64) NOT NULL, 
  hall_code character varying(64) NOT NULL, 
  capacity character varying(20) NOT NULL, 
  booking_date date NOT NULL, 
  booking_from_time time without time zone NOT NULL, 
  booking_to_time time without time zone NOT NULL, 
  status character varying(30) NOT NULL, 
  createdby character varying(64) NOT NULL, 
  createdtime bigint NOT NULL, 
  lastmodifiedby character varying(64), 
  lastmodifiedtime bigint
);
