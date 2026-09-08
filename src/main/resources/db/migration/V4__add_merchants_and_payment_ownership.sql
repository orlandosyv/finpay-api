CREATE TABLE dbo.merchants (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL,
    updated_at DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_merchants PRIMARY KEY (id),
    CONSTRAINT CK_merchants_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);
GO

INSERT INTO dbo.merchants (name, status, created_at, updated_at)
VALUES ('Demo Merchant', 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME());
GO

ALTER TABLE dbo.payments
ADD merchant_id BIGINT NULL;
GO

UPDATE dbo.payments
SET merchant_id = (
    SELECT TOP (1) id
    FROM dbo.merchants
    WHERE name = 'Demo Merchant'
    ORDER BY id
)
WHERE merchant_id IS NULL;
GO

ALTER TABLE dbo.payments
ALTER COLUMN merchant_id BIGINT NOT NULL;
GO

ALTER TABLE dbo.payments
ADD CONSTRAINT FK_payments_merchants
    FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id);
GO

CREATE INDEX IX_payments_merchant_id
ON dbo.payments (merchant_id);
