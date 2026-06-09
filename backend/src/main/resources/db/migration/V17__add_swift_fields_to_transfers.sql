-- Dodaje pola specyficzne dla przelewów SWIFT do tabeli transfers.
-- Dla kanałów innych niż SWIFT kolumny pozostają NULL.

ALTER TABLE transfers
    ADD COLUMN swift_msg_id          VARCHAR(64),    -- MsgId z nagłówka pacs.008
    ADD COLUMN swift_uetr            VARCHAR(36),    -- UETR (UUID end-to-end tracking)
    ADD COLUMN swift_charge_bearer   VARCHAR(4),     -- SHA/OUR/BEN/CRED
    ADD COLUMN swift_route           TEXT,           -- JSON: ["BANKDEXX","UKBKGB01XXX","USBKUS01XXX"]
    ADD COLUMN swift_fee             DECIMAL(19, 4), -- opłata pobrana od klienta (EUR)
    ADD COLUMN swift_fx_rate         DECIMAL(19, 8), -- kurs EUR → waluta docelowa
    ADD COLUMN swift_target_currency VARCHAR(3),     -- waluta docelowa SWIFT (np. USD)
    ADD COLUMN swift_recalled        BOOLEAN         NOT NULL DEFAULT FALSE;
