CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    parent_account_id UUID REFERENCES accounts(id),
    account_number VARCHAR(34) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(19, 4) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_account_type CHECK (type IN ('STANDARD', 'JUNIOR')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_junior_has_parent CHECK (
        type != 'JUNIOR' OR parent_account_id IS NOT NULL
    )
);
