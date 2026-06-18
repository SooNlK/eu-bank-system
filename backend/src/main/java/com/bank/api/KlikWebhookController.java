package com.bank.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kontroler do obsługi żądań przychodzących (webhooków) z systemu KLIK.
 */
@RestController
@RequestMapping("/api/v1/klik")
@Tag(name = "KLIK Webhook", description = "Odbiór webhooków i testów połączenia z systemu KLIK")
public class KlikWebhookController {

    private static final Logger log = LoggerFactory.getLogger(KlikWebhookController.class);

    /**
     * Test połączenia (liveness check) ze strony serwera KLIK.
     */
    @PostMapping("/ping")
    @Operation(summary = "Ping weryfikacyjny", description = "Test poprawności połączenia i aktywności webhooka banku.")
    public ResponseEntity<PingResponse> ping(@RequestBody PingRequest request) {
        log.info("Received ping from KLIK: timestamp={}, nonce={}", request.timestamp(), request.nonce());
        return ResponseEntity.ok(new PingResponse(request.timestamp(), request.nonce(), true));
    }

    /**
     * Szkielet webhooka autoryzacyjnego (A3 w diagramach sekwencji).
     * Zostanie w pełni zaimplementowany w Kroku 1 integracji.
     */
    @PostMapping("/authorize")
    @Operation(summary = "Webhook autoryzacyjny", description = "Zlecenie autoryzacji płatności kodem KLIK (przepływ C2B).")
    public ResponseEntity<AuthorizeResponse> authorize(@RequestBody AuthorizeRequest request) {
        log.info("Received C2B authorization request from KLIK: transactionId={}, userId={}, amount={} {}", 
                request.transactionId(), request.userId(), request.amount(), request.currency());
        
        // Szkielet: zwracamy 200 OK informujące o przyjęciu zlecenia do pokazania klientowi
        return ResponseEntity.ok(new AuthorizeResponse(true, true));
    }

    // ==========================================
    // Rekordy DTO
    // ==========================================

    public record PingRequest(
            String timestamp,
            String nonce
    ) {}

    public record PingResponse(
            String timestamp,
            String nonce,
            boolean pong
    ) {}

    public record AuthorizeRequest(
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("user_id") String userId,
            double amount,
            String currency,
            @JsonProperty("merchant_name") String merchantName,
            @JsonProperty("is_on_us") boolean isOnUs,
            @JsonProperty("expiry_time") String expiryTime,
            String zone
    ) {}

    public record AuthorizeResponse(
            boolean received,
            @JsonProperty("will_prompt_user") boolean willPromptUser
    ) {}
}
