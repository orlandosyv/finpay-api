CREATE TABLE dbo.payment_idempotency_keys (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    merchant_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    payment_id BIGINT NOT NULL,
    original_amount DECIMAL(19, 2) NOT NULL,
    original_currency VARCHAR(3) NOT NULL,
    original_status VARCHAR(20) NOT NULL,
    original_created_at DATETIMEOFFSET(7) NOT NULL,
    original_updated_at DATETIMEOFFSET(7) NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_payment_idempotency_keys PRIMARY KEY (id),
    CONSTRAINT UQ_payment_idempotency_merchant_key
        UNIQUE (merchant_id, idempotency_key),
    CONSTRAINT UQ_payment_idempotency_payment UNIQUE (payment_id),
    CONSTRAINT CK_payment_idempotency_status
        CHECK (original_status IN ('PENDING', 'APPROVED', 'DECLINED', 'REFUNDED')),
    CONSTRAINT FK_payment_idempotency_merchants
        FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id),
    CONSTRAINT FK_payment_idempotency_payments
        FOREIGN KEY (payment_id) REFERENCES dbo.payments(id) ON DELETE CASCADE
);
