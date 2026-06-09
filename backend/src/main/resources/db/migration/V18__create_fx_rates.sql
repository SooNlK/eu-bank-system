-- Sztywne kursy walut banku (EUR jako waluta bazowa BANKDEXX).
-- W środowisku produkcyjnym kursy byłyby aktualizowane z zewnętrznych źródeł.

CREATE TABLE fx_rates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency   VARCHAR(3) NOT NULL,
    rate          DECIMAL(19, 8) NOT NULL,
    effective_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (from_currency, to_currency)
);

-- Seed: kursy walut (EUR jako baza)
-- EUR pary
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('EUR', 'EUR', 1.00000000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('EUR', 'USD', 1.08000000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('EUR', 'GBP', 0.85920000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('EUR', 'PLN', 4.27000000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('EUR', 'CHF', 0.97000000);

-- USD pary
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('USD', 'EUR', 0.92590000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('USD', 'USD', 1.00000000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('USD', 'GBP', 0.79560000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('USD', 'PLN', 3.95000000);

-- GBP pary
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('GBP', 'EUR', 1.16390000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('GBP', 'USD', 1.25700000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('GBP', 'GBP', 1.00000000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('GBP', 'PLN', 4.97000000);

-- PLN pary
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('PLN', 'EUR', 0.23410000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('PLN', 'USD', 0.25310000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('PLN', 'GBP', 0.20120000);
INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES ('PLN', 'PLN', 1.00000000);
