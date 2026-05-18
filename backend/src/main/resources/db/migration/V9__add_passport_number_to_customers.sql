ALTER TABLE customers
ADD COLUMN passport_number VARCHAR(20);

UPDATE customers
SET passport_number = 'LEGACY-' || SUBSTRING(REPLACE(id::TEXT, '-', ''), 1, 12)
WHERE passport_number IS NULL;

ALTER TABLE customers
ALTER COLUMN passport_number SET NOT NULL;

ALTER TABLE customers
ADD CONSTRAINT uq_customers_passport_number UNIQUE (passport_number);
