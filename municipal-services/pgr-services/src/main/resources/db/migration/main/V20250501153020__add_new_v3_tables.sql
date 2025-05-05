
CREATE TABLE IF NOT EXISTS ug_pgr_service_v3
(
    id character varying(64),
    tenantid character varying(256) NOT NULL,
    servicecode character varying(256) NOT NULL,
    servicerequestid character varying(256) NOT NULL,
    description character varying(4000),
    accountid character varying(256),
    additionaldetails jsonb,
    applicationstatus character varying(128),
    rating smallint,
    source character varying(256),
	createdby character varying(256) NOT NULL,
    createdtime bigint NOT NULL,
    lastmodifiedby character varying(256),
    lastmodifiedtime bigint,
    active boolean DEFAULT true,
    serviceType character varying(256),
    inputgrievance character varying(256),
    CONSTRAINT pk_ug_pgr_service_v3 PRIMARY KEY (tenantid, servicerequestid),
    CONSTRAINT uk_ug_pgr_service_v3 UNIQUE (id)
);

CREATE INDEX IF NOT EXISTS index_ug_pgr_service_v3_accountid
    ON ug_pgr_service_v3 USING btree
    (accountid ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS index_ug_pgr_service_v3_applicationstatus
    ON ug_pgr_service_v3 USING btree
    (applicationstatus ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS index_ug_pgr_service_v3_id
    ON ug_pgr_service_v3 USING btree
    (id ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS index_ug_pgr_service_v3_servicecode
    ON ug_pgr_service_v3 USING btree
    (servicecode ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS index_ug_pgr_service_v3_tenantid_servicerequestid
    ON ug_pgr_service_v3 USING btree
    (tenantid ASC NULLS LAST, servicerequestid ASC NULLS LAST);



CREATE TABLE IF NOT EXISTS ug_pgr_address_v3
(
    tenantid character varying(256) NOT NULL,
    id character varying(256) NOT NULL,
    parentid character varying(256) NOT NULL,
    doorno character varying(128), 
    plotno character varying(256), 
    buildingname character varying(1024), 
    street character varying(1024),
    landmark character varying(1024),
    city character varying(512),
    pincode character varying(16),
    locality character varying(128) NOT NULL,
    district character varying(256),
    region character varying(256),
    state character varying(256),
    country character varying(512),
    latitude numeric(9,6),
    longitude numeric(10,7),
    createdby character varying(128) NOT NULL,
    createdtime bigint NOT NULL,
    lastmodifiedby character varying(128),
    lastmodifiedtime bigint,
    additionaldetails jsonb,
    CONSTRAINT pk_eg_pgr_address_v3 PRIMARY KEY (id),
    CONSTRAINT fk_eg_pgr_address_v3 FOREIGN KEY (parentid)
        REFERENCES ug_pgr_service_v3 (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE INDEX IF NOT EXISTS index_ug_pgr_address_v3_locality
    ON ug_pgr_address_v3 USING btree
    (locality ASC NULLS LAST);