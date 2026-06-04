package com.bank.api;

import com.bank.config.EuPaymentsProperties;
import com.bank.service.TransferService;
import com.bank.client.eupayments.SepaInstantClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class TargetWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TargetWebhookController.class);

    private final TransferService transferService;
    private final SepaInstantClient sepaInstantClient;
    private final EuPaymentsProperties props;

    public TargetWebhookController(
            TransferService transferService,
            SepaInstantClient sepaInstantClient,
            EuPaymentsProperties props
    ) {
        this.transferService = transferService;
        this.sepaInstantClient = sepaInstantClient;
        this.props = props;
    }

    @PostMapping("/target-settlement")
    public ResponseEntity<String> handleTargetWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received TARGET webhook event: {} for transfer {}", payload.event(), payload.transferId());

        // 1. Weryfikacja sygnatury HMAC
        String computedSignature = computeSignature(payload, props.webhookSecret());
        if (!computedSignature.equals(payload.signature())) {
            log.warn("Webhook signature mismatch! Got: {}, Expected: {}", payload.signature(), computedSignature);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // 2. Obsługa zdarzenia
        try {
            String receiverIban = payload.receiverIban();
            String senderIban = payload.senderIban();

            // Jeśli IBAN-y są puste (jak w SEPA Instant), próbujemy odnaleźć szczegóły w symulatorze
            if ((receiverIban == null || receiverIban.isBlank()) && "payment.settled".equals(payload.event())) {
                if (payload.transferId() != null && payload.transferId().startsWith("NETT-")) {
                    log.info("SEPA Batch netting settlement received. No customer accounts to credit directly. Net amount: {} {}",
                            payload.amount(), payload.currency());
                    return ResponseEntity.ok("Netting settlement logged");
                }

                log.info("SEPA Instant webhook received with null IBANs. Querying simulator transfers list...");
                List<SepaInstantClient.InstantTransferItem> transfers = sepaInstantClient.getTransfers();
                Optional<SepaInstantClient.InstantTransferItem> match = transfers.stream()
                        .filter(t -> payload.transferId().equals(t.transferId()))
                        .findFirst();

                if (match.isPresent()) {
                    receiverIban = match.get().receiverIban();
                    senderIban = match.get().senderIban();
                    log.info("Matched SEPA Instant transfer details: receiverIban={}, senderIban={}", receiverIban, senderIban);
                } else {
                    log.warn("SEPA Instant transfer {} details not found in simulator", payload.transferId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer details not found in simulator");
                }
            }

            if (receiverIban == null || receiverIban.isBlank()) {
                log.warn("Missing receiver IBAN for event {}", payload.event());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing receiver IBAN");
            }

            // Sprawdzamy czy to przelew przychodzący do naszego banku
            if (!props.bankBic().equals(payload.receiverBic())) {
                log.warn("Received webhook for another bank: receiverBic={}", payload.receiverBic());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect receiver BIC");
            }

            String originalDescription = payload.description() != null && !payload.description().isBlank()
                    ? payload.description()
                    : "Przelew zewnętrzny przychodzący";
            String displayDescription = senderIban != null && !senderIban.isBlank()
                    ? originalDescription + " (Od: " + senderIban + ")"
                    : originalDescription;

            transferService.processIncomingTransfer(
                    payload.transferId(),
                    payload.senderBic(),
                    senderIban,
                    receiverIban,
                    BigDecimal.valueOf(payload.amount()),
                    payload.currency(),
                    displayDescription
            );

            return ResponseEntity.ok("Transfer processed successfully");

        } catch (Exception e) {
            log.error("Failed to process incoming transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private String computeSignature(WebhookPayload payload, String secret) {
        String data = buildSignatureBase(payload);
        log.debug("Signature base data: {}", data);
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    private String buildSignatureBase(WebhookPayload payload) {
        return String.format(
                "{\"amount\": %s, \"currency\": %s, \"description\": %s, \"event\": %s, " +
                "\"receiver_bic\": %s, \"receiver_iban\": %s, \"sender_bic\": %s, \"sender_iban\": %s, " +
                "\"settled_at\": %s, \"transfer_id\": %s}",
                Double.toString(payload.amount()),
                quoteOrNull(payload.currency()),
                quoteOrNull(payload.description()),
                quoteOrNull(payload.event()),
                quoteOrNull(payload.receiverBic()),
                quoteOrNull(payload.receiverIban()),
                quoteOrNull(payload.senderBic()),
                quoteOrNull(payload.senderIban()),
                quoteOrNull(payload.settledAt()),
                quoteOrNull(payload.transferId())
        );
    }

    private String quoteOrNull(String val) {
        return val == null ? "null" : "\"" + escapeJson(val) + "\"";
    }

    private String escapeJson(String string) {
        if (string == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    public record WebhookPayload(
            String event,
            @JsonProperty("transfer_id") String transferId,
            @JsonProperty("sender_bic") String senderBic,
            @JsonProperty("receiver_bic") String receiverBic,
            @JsonProperty("sender_iban") String senderIban,
            @JsonProperty("receiver_iban") String receiverIban,
            double amount,
            String currency,
            String description,
            @JsonProperty("settled_at") String settledAt,
            String signature
    ) {}
}
