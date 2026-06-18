package com.bank.client.klik;

import com.bank.config.KlikProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Klient HTTP do integracji z centralnym systemem płatności KLIK.
 */
@Component
public class KlikClient {

    private static final Logger log = LoggerFactory.getLogger(KlikClient.class);

    private final RestClient restClient;
    private final KlikProperties props;

    public KlikClient(RestClient.Builder restClientBuilder, KlikProperties props) {
        this.props = props;
        this.restClient = restClientBuilder.baseUrl(props.url()).build();
    }

    // ==========================================
    // Moduł C2B (Kody)
    // ==========================================

    /**
     * Generuje 6-cyfrowy kod KLIK dla danego użytkownika.
     */
    public CodeGenerateResponse generateCode(String userId) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Requesting KLIK code generation for user: {} (Zone: {}, Idempotency-Key: {})", 
                userId, props.zone(), idempotencyKey);

        return restClient.post()
                .uri("/codes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-KLIK-Api-Key", props.apiKey())
                .header("Idempotency-Key", idempotencyKey)
                .body(new CodeGenerateRequest(userId, props.zone()))
                .retrieve()
                .body(CodeGenerateResponse.class);
    }

    /**
     * Potwierdza lub odrzuca transakcję KLIK (wywoływane po autoryzacji PIN-em przez klienta).
     */
    public PaymentConfirmResponse confirmPayment(String transactionId, String status, String rejectReason) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Sending payment confirmation to KLIK for transaction: {} (Status: {}, Reason: {}, Idempotency-Key: {})", 
                transactionId, status, rejectReason, idempotencyKey);

        PaymentConfirmRequest body = new PaymentConfirmRequest(transactionId, status, rejectReason);
        return restClient.post()
                .uri("/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-KLIK-Api-Key", props.apiKey())
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .body(PaymentConfirmResponse.class);
    }

    // ==========================================
    // Moduł P2P (Telefony)
    // ==========================================

    /**
     * Rejestruje numer telefonu (alias) przypisany do konta bankowego.
     */
    public AliasRegisterResponse registerAlias(String phone, String iban) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Registering KLIK alias for phone: {} -> IBAN: {} (Zone: {}, Idempotency-Key: {})", 
                phone, iban, props.zone(), idempotencyKey);

        AliasRegisterRequest body = new AliasRegisterRequest(
                phone,
                new AccountIdentifier("iban", iban),
                props.zone()
        );

        return restClient.post()
                .uri("/aliases/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-KLIK-Bank-Api-Key", props.bankApiKey())
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .body(AliasRegisterResponse.class);
    }

    /**
     * Wyszukuje IBAN przypisany do podanego numeru telefonu.
     */
    public AliasLookupResponse lookupAlias(String phone) {
        log.info("Looking up KLIK alias for phone: {}", phone);

        return restClient.get()
                .uri("/aliases/lookup/{phone}", phone)
                .header("X-KLIK-Bank-Api-Key", props.bankApiKey())
                .retrieve()
                .body(AliasLookupResponse.class);
    }

    /**
     * Usuwa rejestrację numeru telefonu (aliasu) klienta.
     */
    public void deleteAlias(String phone) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Deleting KLIK alias for phone: {} (Idempotency-Key: {})", phone, idempotencyKey);

        restClient.delete()
                .uri("/aliases/{phone}", phone)
                .header("X-KLIK-Bank-Api-Key", props.bankApiKey())
                .header("Idempotency-Key", idempotencyKey)
                .retrieve()
                .toBodilessEntity();
    }

    // ==========================================
    // Rekordy DTO
    // ==========================================

    public record CodeGenerateRequest(
            @JsonProperty("user_id") String userId,
            String zone
    ) {}

    public record CodeGenerateResponse(
            String code,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("expires_at") String expiresAt
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentConfirmRequest(
            @JsonProperty("transaction_id") String transactionId,
            String status,
            @JsonProperty("reject_reason") String rejectReason
    ) {}

    public record PaymentConfirmResponse(
            @JsonProperty("transaction_id") String transactionId,
            String status,
            @JsonProperty("amount_gross") String amountGross,
            @JsonProperty("klik_fee") String klikFee,
            @JsonProperty("agent_fee") String agentFee,
            @JsonProperty("merchant_net") String merchantNet,
            String currency,
            @JsonProperty("reject_reason") String rejectReason,
            @JsonProperty("completed_at") String completedAt
    ) {}

    public record AccountIdentifier(
            String type,
            String value
    ) {}

    public record AliasRegisterRequest(
            String phone,
            @JsonProperty("account_identifier") AccountIdentifier accountIdentifier,
            String zone
    ) {}

    public record AliasRegisterResponse(
            @JsonProperty("alias_id") String aliasId,
            String phone,
            @JsonProperty("registered_at") String registeredAt
    ) {}

    public record AliasLookupResponse(
            String phone,
            @JsonProperty("bank_id") String bankId,
            @JsonProperty("bank_code") String bankCode,
            @JsonProperty("account_identifier") AccountIdentifier accountIdentifier
    ) {}
}
