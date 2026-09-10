CREATE TABLE dbo.outbox_events (
    id UNIQUEIDENTIFIER NOT NULL,
    merchant_id BIGINT NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    payload VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL,
    available_at DATETIMEOFFSET(7) NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL,
    processed_at DATETIMEOFFSET(7) NULL,
    CONSTRAINT PK_outbox_events PRIMARY KEY (id),
    CONSTRAINT CK_outbox_events_status
        CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    CONSTRAINT CK_outbox_events_attempts CHECK (attempts >= 0),
    CONSTRAINT FK_outbox_events_merchants
        FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id)
);
GO

CREATE INDEX IX_outbox_events_pending
ON dbo.outbox_events (status, available_at, created_at);
GO

CREATE TABLE dbo.webhook_deliveries (
    id UNIQUEIDENTIFIER NOT NULL,
    event_id UNIQUEIDENTIFIER NOT NULL,
    webhook_endpoint_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_status INT NULL,
    error_message VARCHAR(1000) NULL,
    attempted_at DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_webhook_deliveries PRIMARY KEY (id),
    CONSTRAINT UQ_webhook_delivery_attempt
        UNIQUE (event_id, webhook_endpoint_id, attempt_number),
    CONSTRAINT CK_webhook_deliveries_status
        CHECK (status IN ('SUCCEEDED', 'FAILED')),
    CONSTRAINT CK_webhook_deliveries_attempt CHECK (attempt_number > 0),
    CONSTRAINT FK_webhook_deliveries_events
        FOREIGN KEY (event_id) REFERENCES dbo.outbox_events(id),
    CONSTRAINT FK_webhook_deliveries_endpoints
        FOREIGN KEY (webhook_endpoint_id) REFERENCES dbo.webhook_endpoints(id)
);
GO

CREATE INDEX IX_webhook_deliveries_event
ON dbo.webhook_deliveries (event_id, webhook_endpoint_id, status);
