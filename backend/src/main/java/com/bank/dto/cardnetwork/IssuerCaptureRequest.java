package com.bank.dto.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IssuerCaptureRequest(
        @NotBlank
        @JsonProperty("authorization_code")
        String authorizationCode,

        @NotNull
        @JsonProperty("transaction_id")
        UUID transactionId
) {}
