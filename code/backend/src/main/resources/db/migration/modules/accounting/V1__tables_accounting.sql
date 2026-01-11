
-- acc_currency
CREATE TABLE ${schema}.acc_currency (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_base BOOLEAN NOT NULL DEFAULT FALSE,
    scale INTEGER NOT NULL,
    
    -- AuditableEntity fields
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_acc_currency_code UNIQUE (code)
);

-- acc_account
CREATE TABLE ${schema}.acc_account (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    currency_id BIGINT NOT NULL,
    is_liquid BOOLEAN NOT NULL DEFAULT FALSE,
    account_type VARCHAR(255) NOT NULL,
    
    -- AuditableEntity fields
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_acc_account_name UNIQUE (name),
    CONSTRAINT fk_acc_account_currency FOREIGN KEY (currency_id) REFERENCES ${schema}.acc_currency(id)
);

-- acc_transaction
CREATE TABLE ${schema}.acc_transaction (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(255),
    description VARCHAR(255),
    fx_rate NUMERIC(19, 4), -- Using explicit precision/scale for BigDecimal, assuming typical currency needs
    
    -- AuditableEntity fields
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);

-- acc_journal
CREATE TABLE ${schema}.acc_journal (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    description VARCHAR(255),
    account_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    credit NUMERIC(19, 4) NOT NULL,
    debit NUMERIC(19, 4) NOT NULL,
    
    -- AuditableEntity fields
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_acc_journal_account FOREIGN KEY (account_id) REFERENCES ${schema}.acc_account(id),
    CONSTRAINT fk_acc_journal_transaction FOREIGN KEY (transaction_id) REFERENCES ${schema}.acc_transaction(id)
);

-- acc_node
CREATE TABLE ${schema}.acc_node (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,
    is_placeholder BOOLEAN NOT NULL DEFAULT TRUE,
    account_id BIGINT,
    order_index INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_acc_node_parent FOREIGN KEY (parent_id) REFERENCES ${schema}.acc_node(id),
    CONSTRAINT fk_acc_node_account FOREIGN KEY (account_id) REFERENCES ${schema}.acc_account(id)
);
