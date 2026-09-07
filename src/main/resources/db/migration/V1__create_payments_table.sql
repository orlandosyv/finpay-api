CREATE TABLE dbo.payments (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT PK_payments PRIMARY KEY (id)
);
