CREATE TABLE transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account_id UUID NOT NULL REFERENCES accounts(id),
    to_account_id UUID REFERENCES accounts(id),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_reference_id VARCHAR(255),
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,

    -- Approval flow dla konta Junior
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by UUID REFERENCES customers(id),
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,

    CONSTRAINT chk_transfer_channel CHECK (channel IN (
        'INTERNAL',
        'SEPA',
        'SEPA_INSTANT',
        'TARGET',
        'SWIFT'
    )),
    CONSTRAINT chk_transfer_status CHECK (status IN (
        'PENDING',
        'PENDING_APPROVAL',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'REJECTED'
    ))
);
