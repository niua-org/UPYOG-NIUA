-- ============================================================
-- DDL MIGRATION SCRIPT FOR UPYOG GARBAGE SERVICE REVAMP
-- File: V20260627143000__gc_schema.sql
-- Description: Consolidated 7-table schema as defined in
-- "Garbage Service: Final Design (UPYOG)" specification.
-- ============================================================

-- Clean up legacy sequences if they exist
DROP SEQUENCE IF EXISTS seq_ug_grbg_application_id;
DROP SEQUENCE IF EXISTS seq_ug_grbg_account_id;
DROP SEQUENCE IF EXISTS seq_ug_grbg_account_audit;

-- ============================================================
-- 1. SEQUENCES
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS seq_ug_gc_application_id START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- ============================================================
-- 2. TABLE: ug_gc_application (Main Garbage Application)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_application (
    id                      BIGINT PRIMARY KEY,
    uuid                    VARCHAR(64),
    owner_uuid              VARCHAR(64),
    tenant_uuid             VARCHAR(64),
    application_no          VARCHAR(64) NOT NULL,
    parent_application_no   VARCHAR(64),
    old_garbage_no          VARCHAR(64),
    garbage_no              VARCHAR(64),
    property_id             VARCHAR(64) NOT NULL,
    floor_no                VARCHAR(50),
    status                  VARCHAR(50) NOT NULL, -- PENDING_FOR_VERIFICATION, EDIT_APPLICATION, APPROVED, REJECTED
    connection_status       VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, DISCONNECTED, PERMANENTLY_DISCONNECTED
    arrears                 NUMERIC(12,2) DEFAULT 0.00,
    tenant                  VARCHAR(100) NOT NULL,
    business_service        VARCHAR(100) DEFAULT 'garbage-service',
    channel                 VARCHAR(50),
    due_date                DATE,
    payment_status          VARCHAR(50) DEFAULT 'PENDING',
    payment_amount          NUMERIC(12,2) DEFAULT 0.00,
    approval_date           BIGINT,
    additional_detail       JSONB,
    created_by              VARCHAR(64),
    created_time            BIGINT,
    last_modified_by        VARCHAR(64),
    last_modified_time      BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ug_gc_app_no ON ug_gc_application(application_no);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ug_gc_no ON ug_gc_application(garbage_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_parent_app_no ON ug_gc_application(parent_application_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_prop_id ON ug_gc_application(property_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_owner ON ug_gc_application(owner_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_gc_tenant_uuid ON ug_gc_application(tenant_uuid);
CREATE INDEX IF NOT EXISTS idx_ug_gc_status ON ug_gc_application(status);
CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_status ON ug_gc_application(connection_status);
CREATE INDEX IF NOT EXISTS idx_ug_gc_tenant ON ug_gc_application(tenant);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ug_gc_prop_floor ON ug_gc_application(property_id, floor_no)
    WHERE connection_status != 'PERMANENTLY_DISCONNECTED';

-- ============================================================
-- 3. TABLE: ug_gc_specification (Garbage Details & Spec)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_specification (
    id                  VARCHAR(64) PRIMARY KEY,
    garbage_id          BIGINT NOT NULL,
    old_garbage_no      VARCHAR(64),
    collection_type     VARCHAR(100),
    property_type       VARCHAR(100),
    sub_category        VARCHAR(100),
    sub_category_type   VARCHAR(100),
    category            VARCHAR(100),
    no_of_units         INTEGER DEFAULT 0,
    special_category    VARCHAR(100),
    floor_no            VARCHAR(50),
    is_inheritance      BOOLEAN DEFAULT FALSE,
    created_by          VARCHAR(64),
    created_time        BIGINT,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT fk_ug_gc_spec_garbage_id FOREIGN KEY (garbage_id) REFERENCES ug_gc_application (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ug_gc_spec_garbage_id ON ug_gc_specification(garbage_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_spec_prop_type ON ug_gc_specification(property_type);

-- ============================================================
-- 4. TABLE: ug_gc_document (Uploaded Verification Documents)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_document (
    document_detail_id  VARCHAR(64) PRIMARY KEY,
    garbage_id          BIGINT NOT NULL,
    document_type       VARCHAR(100),
    file_store_id       VARCHAR(100),
    created_by          VARCHAR(64),
    created_time        BIGINT,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT fk_ug_gc_doc_garbage_id FOREIGN KEY (garbage_id) REFERENCES ug_gc_application (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ug_gc_doc_garbage_id ON ug_gc_document(garbage_id);

-- ============================================================
-- 5. TABLE: ug_gc_connection_log (Lifecycle Request Header)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_connection_log (
    id                  VARCHAR(64) PRIMARY KEY,
    garbage_id          BIGINT NOT NULL,
    application_no      VARCHAR(64) NOT NULL,
    request_no          VARCHAR(64) UNIQUE NOT NULL,
    request_type        VARCHAR(30) NOT NULL, -- DISCONNECTION, RECONNECTION, PERMANENT_DISCONNECTION
    scope               VARCHAR(30) DEFAULT 'SELECTED', -- PROPERTY, SELECTED
    property_id         VARCHAR(64),
    workflow_status     VARCHAR(50),
    requested_by_uuid   VARCHAR(64),
    approved_by_uuid    VARCHAR(64),
    approved_time       BIGINT,
    rejection_reason    VARCHAR(500),
    request_snapshot    JSONB,
    status              VARCHAR(50) NOT NULL, -- INITIATED, APPROVED, REJECTED
    reason              VARCHAR(255) NOT NULL,
    reason_details      VARCHAR(500),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    fee_charged         NUMERIC(12,2) DEFAULT 0.00,
    created_by          VARCHAR(64) NOT NULL,
    created_time        BIGINT NOT NULL,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT fk_ug_gc_conn_log_garbage_id FOREIGN KEY (garbage_id) REFERENCES ug_gc_application (id)
);

CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_app_no ON ug_gc_connection_log(application_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_req_no ON ug_gc_connection_log(request_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_prop_id ON ug_gc_connection_log(property_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_type ON ug_gc_connection_log(request_type);
CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_status ON ug_gc_connection_log(status);
CREATE INDEX IF NOT EXISTS idx_ug_gc_conn_created_time ON ug_gc_connection_log(created_time DESC);

-- ============================================================
-- 6. TABLE: ug_gc_connection_log_target (Lifecycle Target Applications)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_connection_log_target (
    id                      VARCHAR(64) PRIMARY KEY,
    connection_log_id       VARCHAR(64) NOT NULL,
    garbage_id              BIGINT NOT NULL,
    application_no          VARCHAR(64) NOT NULL,
    prev_connection_status  VARCHAR(50),
    created_time            BIGINT,
    CONSTRAINT fk_ug_gc_target_conn_id FOREIGN KEY (connection_log_id) REFERENCES ug_gc_connection_log (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ug_gc_target_conn_id ON ug_gc_connection_log_target(connection_log_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_target_app_no ON ug_gc_connection_log_target(application_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_target_garbage_id ON ug_gc_connection_log_target(garbage_id);

-- ============================================================
-- 7. TABLE: ug_gc_monthly_rent_payment (Monthly Billing & Demand Records)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_monthly_rent_payment (
    id                  VARCHAR(64) PRIMARY KEY,
    garbage_id          BIGINT NOT NULL,
    application_no      VARCHAR(64) NOT NULL,
    bill_no             VARCHAR(64) UNIQUE NOT NULL,
    demand_id           VARCHAR(64),
    bill_date           DATE NOT NULL,
    period_from         DATE NOT NULL,
    period_to           DATE NOT NULL,
    bill_amount         NUMERIC(12,2) NOT NULL,
    payment_status      VARCHAR(50) DEFAULT 'PENDING',
    receipt_no          VARCHAR(64),
    paid_amount         NUMERIC(12,2),
    paid_date           BIGINT,
    payment_mode        VARCHAR(50),
    paid_by_uuid        VARCHAR(64),
    penalty_amount      NUMERIC(12,2) DEFAULT 0.00,
    tenant              VARCHAR(100) NOT NULL,
    created_by          VARCHAR(64) NOT NULL,
    created_time        BIGINT NOT NULL,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT fk_ug_gc_monthly_bill_garbage_id FOREIGN KEY (garbage_id) REFERENCES ug_gc_application (id),
    CONSTRAINT uk_ug_gc_monthly_bill_period UNIQUE (garbage_id, period_from)
);

CREATE INDEX IF NOT EXISTS idx_ug_gc_bill_app_no ON ug_gc_monthly_rent_payment(application_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_bill_no ON ug_gc_monthly_rent_payment(bill_no);
CREATE INDEX IF NOT EXISTS idx_ug_gc_bill_demand ON ug_gc_monthly_rent_payment(demand_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_bill_period_from ON ug_gc_monthly_rent_payment(period_from DESC);

-- ============================================================
-- 8. TABLE: ug_gc_bill_generate (Scheduler Execution Logs)
-- ============================================================
CREATE TABLE IF NOT EXISTS ug_gc_bill_generate (
    id                  VARCHAR(64) PRIMARY KEY,
    garbage_id          BIGINT NOT NULL,
    tenant_id           VARCHAR(100) NOT NULL,
    billing_date        DATE NOT NULL,
    billing_period_from BIGINT NOT NULL,
    billing_period_to   BIGINT NOT NULL,
    amount              NUMERIC(12,2) NOT NULL,
    penalty_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    payment_type        VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by          VARCHAR(64),
    created_time        BIGINT,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ug_gc_sched_tenant ON ug_gc_bill_generate(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_sched_garbage ON ug_gc_bill_generate(garbage_id);
CREATE INDEX IF NOT EXISTS idx_ug_gc_sched_status ON ug_gc_bill_generate(status);
