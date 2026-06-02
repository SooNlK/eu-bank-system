package com.bank.client.eupayments;

import java.math.BigDecimal;

/**
 * Buduje uproszczony XML ISO 20022 (pacs.008 / CstmrCdtTrfInitn)
 * wymagany przez symulatory SEPA Batch i SEPA Instant.
 */
public final class IsoXmlBuilder {

    private IsoXmlBuilder() {}

    /**
     * Buduje XML dla przelewu SEPA / SEPA Instant / TARGET.
     *
     * @param endToEndId   unikalny identyfikator end-to-end (np. UUID transferu)
     * @param amount       kwota przelewu
     * @param currency     waluta (np. "EUR")
     * @param debtorIban   IBAN rachunku obciążanego
     * @param creditorIban IBAN rachunku uznawanego
     * @param debtorBic    BIC banku nadawcy
     * @param creditorBic  BIC banku odbiorcy
     * @param remittance   tytuł przelewu
     */
    public static String build(
            String endToEndId,
            BigDecimal amount,
            String currency,
            String debtorIban,
            String creditorIban,
            String debtorBic,
            String creditorBic,
            String remittance
    ) {
        String safeRemittance = remittance == null ? "" : escapeXml(remittance);
        String safeCcy = currency == null ? "EUR" : currency.trim().toUpperCase();
        String safeCreditorBic = creditorBic == null ? "" : creditorBic.trim();

        return "<Document>" +
               "<CstmrCdtTrfInitn>" +
               "<PmtId><EndToEndId>" + escapeXml(endToEndId) + "</EndToEndId></PmtId>" +
               "<Amt><InstdAmt Ccy=\"" + safeCcy + "\">" + amount.toPlainString() + "</InstdAmt></Amt>" +
               "<DbtrAcct><Id><IBAN>" + escapeXml(debtorIban) + "</IBAN></Id></DbtrAcct>" +
               "<CdtrAcct><Id><IBAN>" + escapeXml(creditorIban) + "</IBAN></Id></CdtrAcct>" +
               "<DbtrAgt><FinInstnId><BIC>" + escapeXml(debtorBic) + "</BIC></FinInstnId></DbtrAgt>" +
               (safeCreditorBic.isBlank() ? "" :
                   "<CdtrAgt><FinInstnId><BIC>" + escapeXml(safeCreditorBic) + "</BIC></FinInstnId></CdtrAgt>") +
               "<RmtInf><Ustrd>" + safeRemittance + "</Ustrd></RmtInf>" +
               "</CstmrCdtTrfInitn>" +
               "</Document>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
