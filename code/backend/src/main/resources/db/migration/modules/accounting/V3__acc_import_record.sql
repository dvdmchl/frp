
-- acc_import_record: staging of records fetched from an external source, one row per (connection, external id)
CREATE TABLE ${schema}.acc_import_record (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    external_account_id VARCHAR(255) NOT NULL,
    external_category_id VARCHAR(255),
    record_date DATE NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    note TEXT,
    counterparty VARCHAR(255),
    source_state VARCHAR(20) NOT NULL,
    transfer_link_id VARCHAR(255),
    source_updated_at TIMESTAMP WITH TIME ZONE,
    payload_hash VARCHAR(64) NOT NULL,
    raw_payload JSONB,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    transaction_id BIGINT,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_acc_import_record_external_id UNIQUE (connection_id, external_id),
    CONSTRAINT fk_acc_import_record_connection FOREIGN KEY (connection_id)
        REFERENCES ${schema}.acc_connection(id) ON DELETE CASCADE,
    CONSTRAINT fk_acc_import_record_transaction FOREIGN KEY (transaction_id)
        REFERENCES ${schema}.acc_transaction(id) ON DELETE SET NULL,
    CONSTRAINT ck_acc_import_record_status
        CHECK (status IN ('NEW', 'POSTED', 'SKIPPED', 'ERROR', 'CONFLICT', 'DELETED'))
);

CREATE INDEX ix_acc_import_record_connection_date ON ${schema}.acc_import_record (connection_id, record_date);
