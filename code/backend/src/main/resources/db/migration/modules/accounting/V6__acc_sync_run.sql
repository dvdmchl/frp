-- acc_sync_run: history of the synchronizations of a connection
CREATE TABLE ${schema}.acc_sync_run (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    fetched_count INTEGER NOT NULL DEFAULT 0,
    new_count INTEGER NOT NULL DEFAULT 0,
    updated_count INTEGER NOT NULL DEFAULT 0,
    deleted_count INTEGER NOT NULL DEFAULT 0,
    posted_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,

    CONSTRAINT fk_acc_sync_run_connection FOREIGN KEY (connection_id)
        REFERENCES ${schema}.acc_connection(id) ON DELETE CASCADE,
    CONSTRAINT ck_acc_sync_run_trigger_type CHECK (trigger_type IN ('SCHEDULED', 'MANUAL')),
    CONSTRAINT ck_acc_sync_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED'))
);

CREATE INDEX ix_acc_sync_run_connection_started ON ${schema}.acc_sync_run (connection_id, started_at DESC);

-- schedule of the synchronization
ALTER TABLE ${schema}.acc_connection
    ADD COLUMN sync_interval_minutes INTEGER NOT NULL DEFAULT 360,
    ADD COLUMN next_sync_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN credentials_rejected BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_acc_connection_sync_interval_minutes CHECK (sync_interval_minutes > 0);
