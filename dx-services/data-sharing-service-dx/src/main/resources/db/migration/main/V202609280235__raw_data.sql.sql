CREATE TABLE IF NOT EXISTS ug_pfms_data (
    id UUID PRIMARY KEY,
    state VARCHAR(100),
    ulb VARCHAR(255),
    ward VARCHAR(100),
    module VARCHAR(100),
    data_date DATE,
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
    financial_year VARCHAR(20) NOT NULL,
    function_code VARCHAR(100) NOT NULL,
    scheme_code VARCHAR(100) NOT NULL,

    additionaldetails JSONB,

    status VARCHAR(50) NOT NULL,
    ingestion_date TIMESTAMP,

    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP,

    CONSTRAINT fk_pfms_transaction_data
        FOREIGN KEY (correlation_id)
        REFERENCES ug_pfms_data(id),

    CONSTRAINT uk_pfms_ulb_voucher_number
        UNIQUE (ulb_code_or_pfms_agency_code, voucher_number)
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