-- acc_external_mapping: mapping of external accounts and categories of a connection to accounting accounts
CREATE TABLE ${schema}.acc_external_mapping (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    external_name VARCHAR(255) NOT NULL,
    currency_code VARCHAR(10),
    account_id BIGINT,
    ignored BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_acc_external_mapping_external_id UNIQUE (connection_id, kind, external_id),
    CONSTRAINT fk_acc_external_mapping_connection FOREIGN KEY (connection_id)
        REFERENCES ${schema}.acc_connection(id) ON DELETE CASCADE,
    CONSTRAINT fk_acc_external_mapping_account FOREIGN KEY (account_id)
        REFERENCES ${schema}.acc_account(id) ON DELETE SET NULL,
    CONSTRAINT ck_acc_external_mapping_kind CHECK (kind IN ('ACCOUNT', 'CATEGORY'))
);

-- fallback accounts for records whose category is not mapped
ALTER TABLE ${schema}.acc_connection
    ADD COLUMN fallback_expense_account_id BIGINT,
    ADD COLUMN fallback_revenue_account_id BIGINT,
    ADD CONSTRAINT fk_acc_connection_fallback_expense_account FOREIGN KEY (fallback_expense_account_id)
        REFERENCES ${schema}.acc_account(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_acc_connection_fallback_revenue_account FOREIGN KEY (fallback_revenue_account_id)
        REFERENCES ${schema}.acc_account(id) ON DELETE SET NULL;
