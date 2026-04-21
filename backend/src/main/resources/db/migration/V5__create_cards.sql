CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    last4 VARCHAR(4) NOT NULL,
    type VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    external_card_token VARCHAR(255),
    expires_at DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Limity dla kart prepaid konta Junior
    daily_limit DECIMAL(19, 4),
    monthly_limit DECIMAL(19, 4),

    CONSTRAINT chk_card_type CHECK (type IN ('DEBIT', 'PREPAID')),
    CONSTRAINT chk_card_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))
);
