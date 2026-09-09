CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL,
    updated_at DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT CK_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);
GO

CREATE TABLE dbo.merchant_users (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    merchant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL,
    updated_at DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_merchant_users PRIMARY KEY (id),
    CONSTRAINT UQ_merchant_users_merchant_user UNIQUE (merchant_id, user_id),
    CONSTRAINT CK_merchant_users_role CHECK (role IN ('MERCHANT_ADMIN', 'MERCHANT_USER')),
    CONSTRAINT FK_merchant_users_merchants
        FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id),
    CONSTRAINT FK_merchant_users_users
        FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

CREATE INDEX IX_merchant_users_user_id
ON dbo.merchant_users (user_id);
