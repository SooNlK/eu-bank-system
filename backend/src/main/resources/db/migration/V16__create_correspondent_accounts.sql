-- Rachunki korespondenckie (nostro) naszego banku (BANKDEXX) u banków-korespondentów.
-- Każdy rekord = konto naszego banku prowadzone przez bank obcy w jego walucie.
-- Przykład: konto BANKDEXX w GBP u UKBKGB01XXX (bank brytyjski)

CREATE TABLE correspondent_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correspondent_bic   VARCHAR(11)      NOT NULL,   -- BIC banku partnera
    correspondent_name  VARCHAR(128)     NOT NULL,   -- nazwa banku partnera
    account_number      VARCHAR(64)      NOT NULL UNIQUE, -- nasz numer konta u partnera
    currency            VARCHAR(3)       NOT NULL,   -- waluta konta (GBP, USD, PLN...)
    balance             DECIMAL(19, 4)   NOT NULL DEFAULT 0.00,
    status              VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_corr_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

-- Seed: nostro accounts dla BANKDEXX
-- Konto BANKDEXX w GBP u UK Bank I (UKBKGB01XXX)
INSERT INTO correspondent_accounts (correspondent_bic, correspondent_name, account_number, currency, balance)
VALUES ('UKBKGB01XXX', 'Bank UK 1', 'GB29UKBK60161331926001', 'GBP', 500000.00);

-- Konto BANKDEXX w GBP u UK Bank II (UKBKGB02XXX)
INSERT INTO correspondent_accounts (correspondent_bic, correspondent_name, account_number, currency, balance)
VALUES ('UKBKGB02XXX', 'Bank UK 2', 'GB29UKBK60161331926002', 'GBP', 250000.00);

-- Konto BANKDEXX w USD u US Bank I (USBKUS01XXX)
INSERT INTO correspondent_accounts (correspondent_bic, correspondent_name, account_number, currency, balance)
VALUES ('USBKUS01XXX', 'Bank USA 1', 'US12USBK00000000000001', 'USD', 750000.00);

-- Konto BANKDEXX w USD u US Bank II (USBKUS02XXX)
INSERT INTO correspondent_accounts (correspondent_bic, correspondent_name, account_number, currency, balance)
VALUES ('USBKUS02XXX', 'Bank USA 2', 'US12USBK00000000000002', 'USD', 250000.00);

-- Konto BANKDEXX w PLN u Polska Bank I (PLBKPL01XXX)
INSERT INTO correspondent_accounts (correspondent_bic, correspondent_name, account_number, currency, balance)
VALUES ('PLBKPL01XXX', 'Bank Polska 1', 'PL61PLBK10901400000700000001', 'PLN', 1000000.00);

-- Konto BANKDEXX w PLN u Polska Bank II (PLBKPL02XXX)
INSERT INTO correspondent_accounts (correspondent_bic, correspondent_name, account_number, currency, balance)
VALUES ('PLBKPL02XXX', 'Bank Polska 2', 'PL61PLBK10901400000700000002', 'PLN', 500000.00);
