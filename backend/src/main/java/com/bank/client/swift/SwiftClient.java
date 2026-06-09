package com.bank.client.swift;

import com.bank.config.SwiftProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Klient HTTP do symulatora sieci SWIFT.
 *
 * Nasz bank: BANKDEXX
 */
@Component
public class SwiftClient {

    private static final Logger log = LoggerFactory.getLogger(SwiftClient.class);

    private final SwiftProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Token cache
    private String cachedToken = null;
    private Instant tokenExpiry = Instant.MIN;

    public SwiftClient(SwiftProperties props) {
        this.props = props;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    // ===== Response DTOs =====

    public record SwiftTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type")   String tokenType,
            @JsonProperty("expires_in")   int expiresIn
    ) {}

    public record SwiftPaymentResponse(
            @JsonProperty("message_id")        String messageId,
            @JsonProperty("status")            String status,        // ACCEPTED / REJECTED / PENDING
            @JsonProperty("route")             List<String> route,
            @JsonProperty("estimated_seconds") double estimatedSeconds,
            @JsonProperty("fee_breakdown")     Map<String, String> feeBreakdown,
            @JsonProperty("error")             String error
    ) {}

    // ===== Public API =====

    public boolean isEnabled() {
        return props.enabled();
    }

    /**
     * Wysyła wiadomość pacs.008 do symulatora SWIFT.
     * Zwraca odpowiedź z trasą, statusem i podziałem opłat.
     */
    public SwiftPaymentResponse sendMessage(String pacs008Xml) {
        if (!props.enabled()) {
            log.warn("SWIFT jest wyłączony w konfiguracji (app.swift.enabled=false)");
            return null;
        }

        String token = getToken();
        if (token == null) {
            log.error("Nie udało się pobrać tokenu SWIFT");
            return null;
        }

        String url = props.url() + "/swift/message";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_XML);

        HttpEntity<String> request = new HttpEntity<>(pacs008Xml, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("SWIFT /swift/message → HTTP {}", response.getStatusCode());
                return objectMapper.readValue(response.getBody(), SwiftPaymentResponse.class);
            } else {
                log.warn("SWIFT /swift/message → HTTP {}: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("Błąd komunikacji ze symulatorem SWIFT: {}", e.getMessage(), e);
            return null;
        }
    }

    // ===== Token management =====

    private synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    private String fetchNewToken() {
        String url = props.url() + "/auth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "client_credentials");
        body.add("client_id",     props.clientId());
        body.add("client_secret", props.clientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SwiftTokenResponse tokenResp = objectMapper.readValue(
                        response.getBody(), SwiftTokenResponse.class);
                cachedToken = tokenResp.accessToken();
                tokenExpiry = Instant.now().plusSeconds(
                        tokenResp.expiresIn() > 0 ? tokenResp.expiresIn() - 30 : 3570);
                log.info("SWIFT token pobrany, ważny do: {}", tokenExpiry);
                return cachedToken;
            }
        } catch (Exception e) {
            log.error("Błąd pobierania tokenu SWIFT z {}: {}", url, e.getMessage(), e);
        }
        return null;
    }

    /** Sprawdza dostępność symulatora SWIFT */
    public boolean isReachable() {
        try {
            restTemplate.getForEntity(props.url() + "/api/openapi.json", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
