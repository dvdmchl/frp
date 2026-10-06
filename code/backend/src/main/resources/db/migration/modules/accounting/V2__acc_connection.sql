
-- acc_connection: connection of the tenant to an external source of accounting data
CREATE TABLE ${schema}.acc_connection (
    id BIGSERIAL PRIMARY KEY,
    connector_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    credentials TEXT,
    credentials_key_version INTEGER,
    sync_settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_successful_sync_at TIMESTAMP WITH TIME ZONE,
    sync_state JSONB,

    -- AuditableEntity fields
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_acc_connection_name UNIQUE (name),
    CONSTRAINT ck_acc_connection_credentials_key_version
        CHECK ((credentials IS NULL) = (credentials_key_version IS NULL))
);
