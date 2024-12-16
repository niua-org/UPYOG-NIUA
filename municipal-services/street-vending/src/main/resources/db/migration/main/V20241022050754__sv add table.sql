CREATE TABLE eg_sv_street_vending_detail
(
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    tenant_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_no character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_date bigint NOT NULL,
    certificate_no character varying(64) COLLATE pg_catalog."default",
    approval_date bigint,
    application_status character varying(50) COLLATE pg_catalog."default" NOT NULL,
    trade_license_no character varying(64) COLLATE pg_catalog."default",
    vending_activity character varying(100) COLLATE pg_catalog."default" NOT NULL,
    vending_zone character varying(100) COLLATE pg_catalog."default" NOT NULL,
    cart_latitude numeric(10,6) NOT NULL,
    cart_longitude numeric(10,6) NOT NULL,
    vending_area integer NOT NULL,
    vending_license_certificate_id character varying(64) COLLATE pg_catalog."default",
    local_authority_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    disability_status character varying(50) COLLATE pg_catalog."default",
    beneficiary_of_social_schemes character varying(100) COLLATE pg_catalog."default",
    terms_and_condition character(1) COLLATE pg_catalog."default" DEFAULT 'Y'::bpchar,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    enrollment_id character varying(100) COLLATE pg_catalog."default",
    payment_receipt_id character varying(64) COLLATE pg_catalog."default",
    vending_license_id character varying(64) COLLATE pg_catalog."default",
    CONSTRAINT eg_sv_street_vending_detail_pkey PRIMARY KEY (application_id),
    CONSTRAINT eg_sv_street_vending_detail_application_no_key UNIQUE (application_no),
    CONSTRAINT eg_sv_street_vending_detail_certificate_no_key UNIQUE (certificate_no)
);


CREATE TABLE eg_sv_vendor_detail
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    vendor_id character varying(64) COLLATE pg_catalog."default",
    name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    father_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    date_of_birth date NOT NULL,
    email_id character varying(200) COLLATE pg_catalog."default",
    mobile_no character varying(100) COLLATE pg_catalog."default" NOT NULL,
    gender character(1) COLLATE pg_catalog."default",
    relationship_type character varying(30) COLLATE pg_catalog."default" NOT NULL,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    user_category character varying(100) COLLATE pg_catalog."default",
    special_category character varying(100) COLLATE pg_catalog."default",
    is_involved boolean,
    CONSTRAINT eg_sv_vendor_detail_pkey PRIMARY KEY (id),
    CONSTRAINT eg_sv_vendor_detail_application_id_fk FOREIGN KEY (application_id)
        REFERENCES public.eg_sv_street_vending_detail (application_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT eg_sv_vendor_detail_vendor_id_fk FOREIGN KEY (vendor_id)
        REFERENCES public.eg_sv_vendor_detail (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);


CREATE TABLE eg_sv_address_detail
(
    address_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    address_type character varying(64) COLLATE pg_catalog."default" NOT NULL,
    vendor_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    house_no character varying(100) COLLATE pg_catalog."default" NOT NULL,
    address_line_1 character varying(150) COLLATE pg_catalog."default" NOT NULL,
    address_line_2 character varying(150) COLLATE pg_catalog."default" NOT NULL,
    landmark character varying(150) COLLATE pg_catalog."default",
    city character varying(100) COLLATE pg_catalog."default" NOT NULL,
    city_code character varying(10) COLLATE pg_catalog."default" NOT NULL,
    locality character varying(100) COLLATE pg_catalog."default" NOT NULL,
    locality_code character varying(100) COLLATE pg_catalog."default" NOT NULL,
    pincode character varying(12) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT eg_sv_address_detail_id_pk PRIMARY KEY (address_id),
    CONSTRAINT eg_sv_address_detail_vendor_id_fk FOREIGN KEY (vendor_id)
        REFERENCES public.eg_sv_vendor_detail (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);


CREATE TABLE eg_sv_bank_detail
(
    bank_detail_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    account_no character varying(64) COLLATE pg_catalog."default",
    ifsc_code character varying(64) COLLATE pg_catalog."default",
    bank_name character varying(300) COLLATE pg_catalog."default",
    bank_branch_name character varying(300) COLLATE pg_catalog."default",
    account_holder_name character varying(300) COLLATE pg_catalog."default",
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    CONSTRAINT eg_sv_bank_detail_pkey PRIMARY KEY (bank_detail_id),
    CONSTRAINT eg_sv_bank_detail_application_id_fk FOREIGN KEY (application_id)
        REFERENCES public.eg_sv_street_vending_detail (application_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE eg_sv_document_detail
(
    document_detail_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    document_type character varying(64) COLLATE pg_catalog."default",
    filestore_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    CONSTRAINT eg_sv_document_detail_id_pk PRIMARY KEY (document_detail_id),
    CONSTRAINT eg_sv_document_detail_application_id_fk FOREIGN KEY (application_id)
        REFERENCES public.eg_sv_street_vending_detail (application_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE eg_sv_operation_time_detail
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    day_of_week character varying(20) COLLATE pg_catalog."default" NOT NULL,
    from_time time without time zone NOT NULL,
    to_time time without time zone NOT NULL,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    CONSTRAINT eg_sv_operation_time_detail_pkey PRIMARY KEY (id),
    CONSTRAINT eg_sv_operation_time_detail_application_id_fk FOREIGN KEY (application_id)
        REFERENCES public.eg_sv_street_vending_detail (application_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE eg_sv_street_vending_detail_auditdetails
(
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    tenant_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_no character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_date bigint NOT NULL,
    certificate_no character varying(64) COLLATE pg_catalog."default",
    approval_date bigint,
    application_status character varying(50) COLLATE pg_catalog."default" NOT NULL,
    trade_license_no character varying(64) COLLATE pg_catalog."default",
    vending_activity character varying(100) COLLATE pg_catalog."default" NOT NULL,
    vending_zone character varying(100) COLLATE pg_catalog."default" NOT NULL,
    cart_latitude numeric(10,6) NOT NULL,
    cart_longitude numeric(10,6) NOT NULL,
    vending_area integer NOT NULL,
    vending_license_certificate_id character varying(64) COLLATE pg_catalog."default",
    local_authority_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    disability_status character varying(50) COLLATE pg_catalog."default",
    beneficiary_of_social_schemes character varying(100) COLLATE pg_catalog."default",
    terms_and_condition character(1) COLLATE pg_catalog."default" DEFAULT 'Y'::bpchar,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    enrollment_id character varying(100) COLLATE pg_catalog."default",
    payment_receipt_id character varying(64) COLLATE pg_catalog."default",
    vending_license_id character varying(64) COLLATE pg_catalog."default"
);


CREATE TABLE eg_sv_street_vending_draft_detail
(
    draft_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    tenant_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    user_uuid character varying(64) COLLATE pg_catalog."default" NOT NULL,
    draft_application_data jsonb NOT NULL,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    CONSTRAINT eg_sv_street_vending_draft_detail_pkey PRIMARY KEY (draft_id)
);

CREATE TABLE eg_sv_vendor_detail_auditdetails
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    application_id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    vendor_id character varying(64) COLLATE pg_catalog."default",
    name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    father_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    date_of_birth date NOT NULL,
    email_id character varying(200) COLLATE pg_catalog."default",
    mobile_no character varying(100) COLLATE pg_catalog."default" NOT NULL,
    gender character(1) COLLATE pg_catalog."default",
    relationship_type character varying(30) COLLATE pg_catalog."default" NOT NULL,
    createdby character varying(64) COLLATE pg_catalog."default" NOT NULL,
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint NOT NULL,
    lastmodifiedtime bigint,
    user_category character varying(100) COLLATE pg_catalog."default",
    special_category character varying(100) COLLATE pg_catalog."default",
    is_involved boolean
);