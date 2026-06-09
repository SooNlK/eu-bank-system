package com.bank.dto.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record IssuerCaptureRequest(
        @NotBlank
        @JsonProperty("authorization_code")
        String authorizationCode,

        @NotNull
        @JsonProperty("transaction_id")
        UUID transactionId,

        @JsonProperty("amount")
        BigDecimal amount,

        @JsonProperty("currency")
        String currency,

        @JsonProperty("merchant_id")
        String merchantId,

        @JsonProperty("card_token")
        String cardToken
) {
    public IssuerCaptureRequest(String authorizationCode, UUID transactionId) {
        this(authorizationCode, transactionId, null, null, null, null);
    }
}
