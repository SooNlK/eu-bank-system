package com.bank.api;

import com.bank.service.SwiftIncomingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint przyjmowania przychodzących wiadomości SWIFT od symulatora.
 *
 * Symulator SWIFT (Jkwasnyy) wysyła pacs.008 XML na EU_BANK_URL = http://backend:8080/receive
 * Endpoint jest publiczny (bez JWT) – symulator nie wysyła tokenów JWT.
 */
@RestController
@Tag(name = "SWIFT Webhook", description = "Odbiór przychodzących przelewów SWIFT z symulatora (bez autoryzacji JWT)")
public class SwiftWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SwiftWebhookController.class);

    private final SwiftIncomingService swiftIncomingService;
    private final com.bank.config.SwiftProperties swiftProperties;

    public SwiftWebhookController(SwiftIncomingService swiftIncomingService, com.bank.config.SwiftProperties swiftProperties) {
        this.swiftIncomingService = swiftIncomingService;
        this.swiftProperties = swiftProperties;
    }

    /**
     * Główny endpoint odbioru przelewów SWIFT.
     * Symulator wywołuje: POST EU_BANK_URL (domyślnie /receive)
     */
    @PostMapping(
            value = "/receive",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, "application/xml;charset=UTF-8", "*/*"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Odbiór wiadomości pacs.008", 
            description = "Główny endpoint webhooka dla symulatora SWIFT. Przyjmuje i rozlicza pacs.008 XML.")
    public ResponseEntity<String> receiveSwiftMessage(
            @RequestBody String pacs008Xml,
            @RequestHeader(value = "X-SWIFT-Message-Type", required = false) String messageType,
            @RequestHeader(value = "X-SWIFT-Fee-For", required = false) String feeFor,
            @RequestHeader(value = "X-SWIFT-Fee-Amount", required = false) String feeAmount
    ) {
        log.info("SWIFT /receive – odebrano wiadomość pacs.008 ({} znaków), typ={}, feeFor={}", pacs008Xml.length(), messageType, feeFor);

        if (feeFor != null && !feeFor.isBlank()) {
            log.info("SWIFT /receive – odebrano powiadomienie o prowizji (Fee Notification) dla roli: {}, kwota: {}", feeFor, feeAmount);
            return ResponseEntity.ok("{\"status\":\"ACCEPTED\"}");
        }

        try {
            boolean isReturn = "RETURN".equalsIgnoreCase(messageType);
            String result = swiftIncomingService.processIncoming(pacs008Xml, isReturn);
            if ("ACCEPTED".equals(result)) {
                return ResponseEntity.ok("{\"status\":\"ACCEPTED\"}");
            } else {
                // Konto nieistnieje – Recall wysłany
                return ResponseEntity.ok("{\"status\":\"REJECTED\",\"reason\":\"Account not found or inactive\"}");
            }
        } catch (IllegalArgumentException e) {
            log.warn("SWIFT /receive – błąd parsowania XML: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"status\":\"ERROR\",\"reason\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            log.error("SWIFT /receive – błąd wewnętrzny: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"status\":\"ERROR\",\"reason\":\"Internal error\"}");
        }
    }

    /**
     * Dodatkowy alias na /api/v1/swift/receive – dla przejrzystości API.
     */
    @PostMapping(
            value = "/api/v1/swift/receive",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, "*/*"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Alias odbioru wiadomości pacs.008", 
            description = "Dodatkowy endpoint /api/v1/swift/receive do odbierania wiadomości SWIFT.")
    public ResponseEntity<String> receiveSwiftMessageV1(
            @RequestBody String pacs008Xml,
            @RequestHeader(value = "X-SWIFT-Message-Type", required = false) String messageType,
            @RequestHeader(value = "X-SWIFT-Fee-For", required = false) String feeFor,
            @RequestHeader(value = "X-SWIFT-Fee-Amount", required = false) String feeAmount
    ) {
        return receiveSwiftMessage(pacs008Xml, messageType, feeFor, feeAmount);
    }

    /**
     * Health check – symulator może sprawdzić dostępność banku.
     */
    @GetMapping("/receive")
    @Operation(summary = "Health check SWIFT", 
            description = "Sprawdza status integracji SWIFT dla symulatora.")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"bank\":\"" + swiftProperties.bankBic() + "\"}");
    }
}
