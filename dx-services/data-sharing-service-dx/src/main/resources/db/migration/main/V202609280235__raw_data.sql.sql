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