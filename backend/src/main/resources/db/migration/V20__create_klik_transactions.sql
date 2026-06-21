CREATE TABLE klik_transactions (
    id UUID PRIMARY KEY, -- transaction_id z systemu KLIK
    account_id UUID NOT NULL REFERENCES accounts(id),
    amount DECIMAL(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, COMPLETED, REJECTED
    reject_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
