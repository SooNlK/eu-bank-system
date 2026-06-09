ALTER TABLE transactions
    ADD COLUMN card_id UUID REFERENCES cards(id);
