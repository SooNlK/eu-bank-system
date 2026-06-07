package com.bank.client.eupayments;

import com.bank.config.EuPaymentsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
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
 * Klient HTTP do serwisu SEPA Batch (SCT) – port 8002.
 * Symulator przyjmuje XML ISO 20022 i zwraca XML CstmrPmtStsRpt.
 * Przelewy trafiają do kolejki sesji (~5 min).
 */
@Component
public class SepaBatchClient {

    private static final Logger log = LoggerFactory.getLogger(SepaBatchClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SepaBatchClient(EuPaymentsProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
        ));
        this.baseUrl = props.sepaBatchUrl();
    }

    /**
     * Wysyła przelew SEPA Batch jako XML ISO 20022.
     */
    public SepaBatchResponse submitTransfer(String xml) {
        log.debug("SEPA Batch: wysyłam przelew do kolejki, URL: {}", baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.ALL));
        HttpEntity<String> request = new HttpEntity<>(xml, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/transfers/xml", request, String.class);

        String body = response.getBody();
        log.debug("SEPA Batch: odpowiedź HTTP {}: {}", response.getStatusCode(), body);
        return parseStatusResponse(body);
    }

    private SepaBatchResponse parseStatusResponse(String xml) {
        if (xml == null || xml.isBlank()) {
            return new SepaBatchResponse(null, "FAILED", null, null);
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            String txSts     = getText(doc, "TxSts");
            String endToEndId = getText(doc, "OrgnlEndToEndId");
            String sessionId  = getText(doc, "SessionId");

            // SEPA Batch: ACCP = w kolejce (Accepted Customer Profile)
            // ACSC = rozliczone (Accepted Settlement Completed)
            String status = switch (txSts == null ? "" : txSts.toUpperCase()) {
                case "ACSC", "ACCC"        -> "COMPLETED";
                case "RJCT"                -> "FAILED";
                case "ACCP", "PDNG", "RCVD" -> "QUEUED";
                default                    -> "QUEUED";
            };

            log.info("SEPA Batch: TxSts={} → status={}, sessionId={}", txSts, status, sessionId);
            return new SepaBatchResponse(endToEndId, status, sessionId, txSts);

        } catch (Exception e) {
            log.error("SEPA Batch: błąd parsowania XML odpowiedzi: {}", e.getMessage());
            return new SepaBatchResponse(null, "FAILED", null, null);
        }
    }

    private String getText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    public record SepaBatchResponse(
            String endToEndId,
            String status,
            String sessionId,
            String rawTxSts
    ) {}
}
