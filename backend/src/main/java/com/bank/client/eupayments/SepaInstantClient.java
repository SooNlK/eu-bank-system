package com.bank.client.eupayments;

import com.bank.config.EuPaymentsProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Klient HTTP do serwisu SEPA Instant (SCT Inst) – port 8003.
 * Symulator przyjmuje XML ISO 20022 i zwraca XML CstmrPmtStsRpt.
 * TxSts=ACSC → sukces, TxSts=RJCT → odrzucenie.
 */
@Component
public class SepaInstantClient {

    private static final Logger log = LoggerFactory.getLogger(SepaInstantClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SepaInstantClient(EuPaymentsProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(new ObjectMapper());
        jacksonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.ALL));
        this.restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                jacksonConverter
        ));
        this.baseUrl = props.sepaInstantUrl();
    }

    /**
     * Wysyła przelew SEPA Instant jako XML ISO 20022.
     * @return odpowiedź ze statusem COMPLETED / FAILED
     */
    public SepaPaymentResponse submitTransfer(String xml) {
        log.debug("SEPA Instant: wysyłam przelew XML do {}", baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.ALL));
        HttpEntity<String> request = new HttpEntity<>(xml, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/transfers/xml", request, String.class);

        String body = response.getBody();
        log.debug("SEPA Instant: odpowiedź HTTP {}: {}", response.getStatusCode(), body);
        return parseStatusResponse(body);
    }

    /**
     * Parsuje ISO 20022 CstmrPmtStsRpt z odpowiedzi symulatora.
     * Przykład:
     * <Document><CstmrPmtStsRpt><OrgnlPmtInfAndSts>
     *   <OrgnlEndToEndId>uuid</OrgnlEndToEndId>
     *   <TxSts>ACSC</TxSts>
     * </OrgnlPmtInfAndSts></CstmrPmtStsRpt></Document>
     */
    private SepaPaymentResponse parseStatusResponse(String xml) {
        if (xml == null || xml.isBlank()) {
            log.warn("SEPA Instant: pusta odpowiedź");
            return new SepaPaymentResponse(null, "FAILED", null, null);
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            String txSts = getText(doc, "TxSts");
            String endToEndId = getText(doc, "OrgnlEndToEndId");
            String sessionId  = getText(doc, "SessionId");

            // Mapowanie statusów ISO 20022 na wewnętrzne statusy
            // ACSC = Accepted Settlement Completed
            // ACCP = Accepted Customer Profile (w kolejce)
            // RJCT = Rejected
            String status = switch (txSts == null ? "" : txSts.toUpperCase()) {
                case "ACSC", "ACCC" -> "COMPLETED";
                case "RJCT"         -> "FAILED";
                default             -> "PROCESSING";
            };

            log.info("SEPA Instant: TxSts={} → status={}, endToEndId={}", txSts, status, endToEndId);
            return new SepaPaymentResponse(endToEndId, status, sessionId, txSts);

        } catch (Exception e) {
            log.error("SEPA Instant: błąd parsowania XML odpowiedzi: {}", e.getMessage());
            return new SepaPaymentResponse(null, "FAILED", null, null);
        }
    }

    private String getText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    public record SepaPaymentResponse(
            String endToEndId,
            String status,
            String sessionId,
            String rawTxSts
    ) {}

    public record InstantTransferItem(
            @JsonProperty("transfer_id") String transferId,
            @JsonProperty("sender_iban") String senderIban,
            @JsonProperty("receiver_iban") String receiverIban,
            @JsonProperty("sender_bic") String senderBic,
            @JsonProperty("receiver_bic") String receiverBic,
            @JsonProperty("amount") double amount,
            @JsonProperty("status") String status,
            @JsonProperty("error_message") String errorMessage,
            @JsonProperty("created_at") String createdAt
    ) {}

    public List<InstantTransferItem> getTransfers() {
        try {
            ResponseEntity<InstantTransferItem[]> response = restTemplate.getForEntity(
                    baseUrl + "/transfers", InstantTransferItem[].class);
            InstantTransferItem[] body = response.getBody();
            return body != null ? List.of(body) : List.of();
        } catch (Exception e) {
            log.error("SEPA Instant: nie udało się pobrać listy transferów – {}", e.getMessage());
            return List.of();
        }
    }
}
