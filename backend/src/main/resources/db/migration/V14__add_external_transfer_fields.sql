-- Dodaje pola dla zewnętrznych przelewów SEPA / SEPA Instant / TARGET
-- Dla przelewów wewnętrznych kolumny te pozostają NULL (używany jest to_account_id)

ALTER TABLE transfers
    ADD COLUMN to_iban        VARCHAR(34),
    ADD COLUMN to_bic         VARCHAR(11),
    ADD COLUMN beneficiary_name VARCHAR(255);
