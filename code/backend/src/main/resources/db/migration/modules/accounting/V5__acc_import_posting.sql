-- link of an imported transaction to the record it was posted from
ALTER TABLE ${schema}.acc_transaction
    ADD COLUMN source_connection_id BIGINT,
    ADD COLUMN source_external_id VARCHAR(255),
    ADD CONSTRAINT fk_acc_transaction_source_connection FOREIGN KEY (source_connection_id)
        REFERENCES ${schema}.acc_connection(id) ON DELETE SET NULL;

CREATE INDEX ix_acc_transaction_source ON ${schema}.acc_transaction (source_connection_id, source_external_id);

-- rates computed from imported amounts need more decimals than the amounts
ALTER TABLE ${schema}.acc_transaction ALTER COLUMN fx_rate TYPE NUMERIC(19, 8);

-- amount converted by the source and fingerprint of the transaction as it was posted
ALTER TABLE ${schema}.acc_import_record
    ADD COLUMN base_amount NUMERIC(19, 4),
    ADD COLUMN base_currency_code VARCHAR(10),
    ADD COLUMN posted_hash VARCHAR(64);

CREATE INDEX ix_acc_import_record_transaction ON ${schema}.acc_import_record (transaction_id);
CREATE INDEX ix_acc_import_record_transfer ON ${schema}.acc_import_record (connection_id, transfer_link_id);
