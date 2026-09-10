CREATE TABLE dbo.webhook_endpoints (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    merchant_id BIGINT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    secret_encrypted VARCHAR(1024) NOT NULL,
    active BIT NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL,
    updated_at DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_webhook_endpoints PRIMARY KEY (id),
    CONSTRAINT FK_webhook_endpoints_merchants
        FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id)
);
GO

CREATE INDEX IX_webhook_endpoints_merchant_active
ON dbo.webhook_endpoints (merchant_id, active);
