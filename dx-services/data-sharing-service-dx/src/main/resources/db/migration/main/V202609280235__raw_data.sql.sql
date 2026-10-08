CREATE TABLE IF NOT EXISTS ug_pfms_data (
    id UUID PRIMARY KEY,
    state VARCHAR(100),
    ward VARCHAR(100),
    module VARCHAR(100),
    data_date DATE,
    ulb VARCHAR(255),
    target_destination VARCHAR(255),

    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ug_pfms_transactions (
    id UUID PRIMARY KEY,
    correlation_id UUID NOT NULL,

    ulb_code_or_pfms_agency_code VARCHAR(100) NOT NULL,
    voucher_number VARCHAR(100) NOT NULL,
    voucher_date DATE NOT NULL,
    voucher_type VARCHAR(100) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    account_head_code VARCHAR(100) NOT NULL,
    function_code VARCHAR(100) NOT NULL,
    scheme_code VARCHAR(100) NOT NULL,

    debit_amount DECIMAL(18,2),
    credit_amount DECIMAL(18,2),
    narration_or_description TEXT,
    voucher_status VARCHAR(50),

    ulb_bank_account_number VARCHAR(100),
    ulb_ifsc_code VARCHAR(20),
    instrument_reference VARCHAR(100),
    mode_of_transaction VARCHAR(50),

    beneficiary_or_payee_name VARCHAR(255),
    beneficiary_account_number VARCHAR(100),
    beneficiary_ifsc_code VARCHAR(20),
    beneficiary_type VARCHAR(50),

    challan_number VARCHAR(100),
    from_account VARCHAR(100),
    to_account VARCHAR(100),
    contra_nature VARCHAR(100),
    transfer_instruction_reference VARCHAR(100),
    reference_voucher_number VARCHAR(100),
    adjustment_type VARCHAR(100),
    related_asset_or_liability_code VARCHAR(100),
    debtor_or_creditor_reference VARCHAR(100),
    basis_of_adjustment VARCHAR(255),
    period_covered VARCHAR(100),

    status VARCHAR(50) NOT NULL,
    ingestion_date TIMESTAMP,

    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP,

    CONSTRAINT fk_pfms_transaction_data
        FOREIGN KEY (correlation_id)
        REFERENCES ug_pfms_data(id),

    CONSTRAINT uk_pfms_voucher_number_date
        UNIQUE (voucher_number, voucher_date)
);


CREATE TABLE IF NOT EXISTS ug_pfms_scheduler_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheduler_type  VARCHAR(50)  NOT NULL,  -- INITIATED_FORWARD | FAILED_RETRY
    started_at      TIMESTAMP    NOT NULL,
    ended_at        TIMESTAMP,
    duration_ms     BIGINT,
    status  VARCHAR(32),
    total_picked    INT,
    success_count   INT,
    failed_count    INT,
    created_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50),
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE IF NOT EXISTS ug_external_api_message_detail (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(128) NOT NULL UNIQUE,
    tenant_id VARCHAR(256) NOT NULL,
    external_api_name VARCHAR(256) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    request_time BIGINT NOT NULL,
    response_time BIGINT,
    duration_ms BIGINT,
    status VARCHAR(32) NOT NULL,
    http_status_code INTEGER,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_time BIGINT NOT NULL,
    last_modified_time BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_detail_correlation_id ON ug_external_api_message_detail (correlation_id);
CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_detail_tenant_id ON ug_external_api_message_detail (tenant_id);
CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_detail_status ON ug_external_api_message_detail (status);
CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_detail_created_time ON ug_external_api_message_detail (created_time);

CREATE TABLE IF NOT EXISTS ug_external_api_message_raw_detail (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(128) NOT NULL,
    request_payload JSONB,
    response_payload JSONB,
    payload_size_bytes BIGINT,
    created_time BIGINT NOT NULL,
    last_modified_time BIGINT NOT NULL,
    CONSTRAINT fk_ug_external_api_message_raw_detail_correlation
        FOREIGN KEY (correlation_id) REFERENCES ug_external_api_message_detail (correlation_id)
);

CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_raw_detail_correlation_id ON ug_external_api_message_raw_detail (correlation_id);
CREATE INDEX IF NOT EXISTS idx_ug_external_api_message_raw_detail_created_time ON ug_external_api_message_raw_detail (created_time);

CREATE TABLE IF NOT EXISTS ug_external_api_error_detail (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(128) NOT NULL,
    error_code VARCHAR(256) NOT NULL,
    error_type VARCHAR(32) NOT NULL,
    error_message TEXT NOT NULL,
    created_time BIGINT NOT NULL,
    CONSTRAINT fk_ug_external_api_error_detail_correlation
        FOREIGN KEY (correlation_id) REFERENCES ug_external_api_message_detail (correlation_id)
);

CREATE INDEX IF NOT EXISTS idx_ug_external_api_error_detail_correlation_id ON ug_external_api_error_detail (correlation_id);
CREATE INDEX IF NOT EXISTS idx_ug_external_api_error_detail_created_time ON ug_external_api_error_detail (created_time);



CREATE UNIQUE INDEX IF NOT EXISTS idx_ug_external_api_message_raw_detail_correlation_id_unique ON ug_external_api_message_raw_detail (correlation_id);


CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
