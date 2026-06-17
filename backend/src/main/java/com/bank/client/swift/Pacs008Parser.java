package com.bank.client.swift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;

/**
 * Parser XML pacs.008 dla przychodzących wiadomości SWIFT.
 */
public final class Pacs008Parser {

    private static final Logger log = LoggerFactory.getLogger(Pacs008Parser.class);

    private Pacs008Parser() {}

    public record ParsedPacs008(
            String msgId,
            String uetr,
            String endToEndId,
            BigDecimal amount,
            String currency,
            String senderName,
            String senderIban,
            String senderBic,
            String receiverName,
            String receiverAccount,
            String receiverBic,
            String chargBearer,
            String remittance
    ) {}

    public static ParsedPacs008 parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            String msgId      = text(doc, "MsgId");
            String uetr       = text(doc, "UETR");
            String endToEndId = text(doc, "EndToEndId");
            String remittance = text(doc, "Ustrd");
            String chargBearer = text(doc, "ChrgBr");

            // Amount + currency
            BigDecimal amount = BigDecimal.ZERO;
            String currency = "EUR";
            NodeList amtNodes = doc.getElementsByTagName("IntrBkSttlmAmt");
            if (amtNodes.getLength() == 0) amtNodes = doc.getElementsByTagName("InstdAmt");
            if (amtNodes.getLength() > 0) {
                org.w3c.dom.Element amtEl = (org.w3c.dom.Element) amtNodes.item(0);
                String amtText = amtEl.getTextContent().trim();
                if (!amtText.isEmpty()) amount = new BigDecimal(amtText);
                String ccyAttr = amtEl.getAttribute("Ccy");
                if (ccyAttr != null && !ccyAttr.isBlank()) currency = ccyAttr.trim().toUpperCase();
            }

            // Debtor (sender)
            String senderName = text(doc, "Dbtr", "Nm");
            String senderIban = firstNonEmpty(text(doc, "DbtrAcct", "IBAN"),
                                              text(doc, "DbtrAcct", "Id"),
                                              text(doc, "IBAN"));
            String senderBic  = firstNonEmpty(text(doc, "DbtrAgt", "BICFI"), text(doc, "BICFI"));

            // Creditor (receiver)
            String receiverName    = text(doc, "Cdtr", "Nm");
            String receiverBic     = text(doc, "CdtrAgt", "BICFI");
            String receiverAccount = firstNonEmpty(text(doc, "CdtrAcct", "IBAN"),
                                                   text(doc, "CdtrAcct", "Id"));

            return new ParsedPacs008(msgId, uetr, endToEndId, amount, currency,
                    senderName, senderIban, senderBic,
                    receiverName, receiverAccount, receiverBic,
                    chargBearer, remittance);

        } catch (Exception e) {
            log.error("Błąd parsowania pacs.008: {}", e.getMessage());
            throw new IllegalArgumentException("Nieprawidłowy format XML pacs.008: " + e.getMessage(), e);
        }
    }

    // helpers
    private static String text(Document doc, String tag) {
        NodeList nl = doc.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }

    private static String text(Document doc, String parentTag, String childTag) {
        NodeList parents = doc.getElementsByTagName(parentTag);
        for (int i = 0; i < parents.getLength(); i++) {
            org.w3c.dom.Element parent = (org.w3c.dom.Element) parents.item(i);
            NodeList children = parent.getElementsByTagName(childTag);
            if (children.getLength() > 0) {
                String val = children.item(0).getTextContent().trim();
                if (!val.isEmpty()) return val;
            }
        }
        return "";
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }
}
