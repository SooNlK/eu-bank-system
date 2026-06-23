package com.bank.client.cardnetwork;

import com.bank.domain.card.CardType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class CardNetworkClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String hmacSecret;

    public CardNetworkClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.card-network.base-url:http://localhost:8072}") String baseUrl,
            @Value("${app.card-network.api-key:bank-key-eu-a}") String apiKey,
            @Value("${app.card-network.hmac-secret:secret-eu-a-hmac}") String hmacSecret
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.hmacSecret = hmacSecret;
    }

    public CardNetworkIssueResponse issueCard(UUID userId, UUID accountId, CardType cardType, BigDecimal initialBalance) {
        Map<String, Object> body = new TreeMap<>();
        body.put("account_id", accountId.toString());
        body.put("card_type", cardType.name());
        body.put("initial_balance", initialBalance.doubleValue());
        body.put("user_id", userId.toString());

        SignedBody signedBody = sign(body);

        return restClient.post()
                .uri("/api/v1/cards/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .header("X-Signature", signedBody.signature())
                .header("X-Timestamp", signedBody.timestamp())
                .body(signedBody.jsonBody())
                .retrieve()
                .body(CardNetworkIssueResponse.class);
    }

    public CardNetworkStatusResponse blockCard(String cardToken, String reason) {
        return updateStatus(cardToken, "BLOCKED", reason);
    }

    public CardNetworkStatusResponse unblockCard(String cardToken) {
        return updateStatus(cardToken, "ACTIVE", "");
    }

    public CardNetworkStatusResponse activateCard(String cardToken, String activatedBy) {
        Map<String, Object> body = new TreeMap<>();
        body.put("activated_by", activatedBy);

        SignedBody signedBody = sign(body);

        return restClient.post()
                .uri("/api/v1/cards/{token}/activate", cardToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .header("X-Signature", signedBody.signature())
                .header("X-Timestamp", signedBody.timestamp())
                .body(signedBody.jsonBody())
                .retrieve()
                .body(CardNetworkStatusResponse.class);
    }

    public CardNetworkStatusResponse topUpCard(String cardToken, BigDecimal amount, String currency) {
        Map<String, Object> body = new TreeMap<>();
        body.put("amount", amount.doubleValue());
        body.put("currency", currency);

        SignedBody signedBody = sign(body);

        return restClient.post()
                .uri("/api/v1/cards/{token}/topup", cardToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .header("X-Signature", signedBody.signature())
                .header("X-Timestamp", signedBody.timestamp())
                .body(signedBody.jsonBody())
                .retrieve()
                .body(CardNetworkStatusResponse.class);
    }

    public CardNetworkCardResponse getCard(String cardToken) {
        return restClient.get()
                .uri("/api/v1/cards/{token}", cardToken)
                .retrieve()
                .body(CardNetworkCardResponse.class);
    }

    public CardNetworkFullPanResponse getFullPan(String cardToken) {
        return restClient.get()
                .uri("/api/v1/cards/{token}/full-pan", cardToken)
                .header("X-Admin-Key", "admin-secret-key-2026")
                .retrieve()
                .body(CardNetworkFullPanResponse.class);
    }

    private CardNetworkStatusResponse updateStatus(String cardToken, String status, String reason) {
        Map<String, Object> body = new TreeMap<>();
        body.put("status", status);
        body.put("reason", reason);

        SignedBody signedBody = sign(body);

        return restClient.patch()
                .uri("/api/v1/cards/{token}/status", cardToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .header("X-Signature", signedBody.signature())
                .header("X-Timestamp", signedBody.timestamp())
                .body(signedBody.jsonBody())
                .retrieve()
                .body(CardNetworkStatusResponse.class);
    }

    private SignedBody sign(Map<String, Object> body) {
        try {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String jsonBody = objectMapper.writeValueAsString(body);
            String payload = timestamp + jsonBody;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

            return new SignedBody(jsonBody, timestamp, signature);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize card network request", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign card network request", ex);
        }
    }

    private record SignedBody(String jsonBody, String timestamp, String signature) {}
}
