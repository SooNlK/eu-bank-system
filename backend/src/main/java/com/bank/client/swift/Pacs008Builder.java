package com.bank.client.swift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Buduje wiadomość XML w formacie ISO 20022 pacs.008.001.08
 * (FIToFICustomerCreditTransfer), wymaganą przez symulator SWIFT.
 */
public final class Pacs008Builder {

    private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter D_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Pacs008Builder() {}

    /**
     * Buduje pacs.008 XML.
     *
     * @param msgId          unikalny ID wiadomości (np. "MSG-" + UUID)
     * @param uetr           UETR (UUID end-to-end)
     * @param endToEndId     identyfikator end-to-end (UUID transferu)
     * @param amount         kwota do przesłania (w walucie targetCurrency)
     * @param targetCurrency waluta przelewu (np. "EUR", "USD")
     * @param senderName     nazwa nadawcy
     * @param senderIban     IBAN nadawcy
     * @param senderBic      BIC banku nadawcy
     * @param receiverName   nazwa odbiorcy
     * @param receiverAccount IBAN lub numer konta odbiorcy
     * @param receiverBic    BIC banku odbiorcy
     * @param chargBearer    SHA/OUR/BEN/CRED
     * @param remittance     tytuł przelewu
     * @param settlementDate data rozliczenia
     * @return XML jako String
     */
    public static String build(
            String msgId,
            String uetr,
            String endToEndId,
            BigDecimal amount,
            String targetCurrency,
            String senderName,
            String senderIban,
            String senderBic,
            String receiverName,
            String receiverAccount,
            String receiverBic,
            String chargBearer,
            String remittance,
            LocalDate settlementDate
    ) {
        String now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).format(DT_FMT);
        String settleDate = (settlementDate != null ? settlementDate : LocalDate.now()).format(D_FMT);
        String ccy = targetCurrency == null ? "EUR" : targetCurrency.trim().toUpperCase();
        String cb  = chargBearer == null ? "SHAR" : chargBearer.trim().toUpperCase();
        String amtStr = amount.toPlainString();

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<Document xmlns=\"" + NS + "\">\n" +
               "  <FIToFICstmrCdtTrf>\n" +
               "    <GrpHdr>\n" +
               "      <MsgId>" + x(msgId) + "</MsgId>\n" +
               "      <CreDtTm>" + now + "</CreDtTm>\n" +
               "      <NbOfTxs>1</NbOfTxs>\n" +
               "      <SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>\n" +
               "    </GrpHdr>\n" +
               "    <CdtTrfTxInf>\n" +
               "      <PmtId>\n" +
               "        <InstrId>" + x(msgId) + "</InstrId>\n" +
               "        <EndToEndId>" + x(endToEndId) + "</EndToEndId>\n" +
               "        <UETR>" + x(uetr) + "</UETR>\n" +
               "      </PmtId>\n" +
               "      <IntrBkSttlmAmt Ccy=\"" + ccy + "\">" + amtStr + "</IntrBkSttlmAmt>\n" +
               "      <IntrBkSttlmDt>" + settleDate + "</IntrBkSttlmDt>\n" +
               "      <InstdAmt Ccy=\"" + ccy + "\">" + amtStr + "</InstdAmt>\n" +
               "      <ChrgBr>" + cb + "</ChrgBr>\n" +
               "      <Dbtr><Nm>" + x(senderName) + "</Nm></Dbtr>\n" +
               "      <DbtrAcct><Id><IBAN>" + x(senderIban) + "</IBAN></Id></DbtrAcct>\n" +
               "      <DbtrAgt><FinInstnId><BICFI>" + x(senderBic) + "</BICFI></FinInstnId></DbtrAgt>\n" +
               "      <Cdtr><Nm>" + x(receiverName) + "</Nm></Cdtr>\n" +
               "      <CdtrAgt><FinInstnId><BICFI>" + x(receiverBic) + "</BICFI></FinInstnId></CdtrAgt>\n" +
               creditorAccount(receiverAccount) +
               "      <RmtInf><Ustrd>" + x(remittance != null ? remittance : "") + "</Ustrd></RmtInf>\n" +
               "    </CdtTrfTxInf>\n" +
               "  </FIToFICstmrCdtTrf>\n" +
               "</Document>";
    }

    /** IBAN lub zwykły numer konta (Othr/Id) */
    private static String creditorAccount(String account) {
        if (account == null || account.isBlank()) return "";
        boolean looksLikeIban = account.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}");
        if (looksLikeIban) {
            return "      <CdtrAcct><Id><IBAN>" + x(account) + "</IBAN></Id></CdtrAcct>\n";
        } else {
            return "      <CdtrAcct><Id><Othr><Id>" + x(account) + "</Id></Othr></Id></CdtrAcct>\n";
        }
    }

    private static String x(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
