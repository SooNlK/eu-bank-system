package com.bank.dto.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record IssuerAuthorizationRequest(
        @NotNull
        @JsonProperty("account_id")
        UUID accountId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotNull
        @JsonProperty("transaction_id")
        UUID transactionId,

        @JsonProperty("merchant_name")
        String merchantName
) {}
