ALTER TABLE dbo.payments
ADD created_at DATETIME2(6) NULL,
    updated_at DATETIME2(6) NULL;
GO

UPDATE dbo.payments
SET created_at = SYSUTCDATETIME(),
    updated_at = SYSUTCDATETIME()
WHERE created_at IS NULL
   OR updated_at IS NULL;
GO

ALTER TABLE dbo.payments
ALTER COLUMN created_at DATETIME2(6) NOT NULL;
GO

ALTER TABLE dbo.payments
ALTER COLUMN updated_at DATETIME2(6) NOT NULL;
