package com.bank.dto.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssuerAuthorizationResponse(
        @JsonProperty("authorization_code")
        String authorizationCode,
        String status,
        @JsonProperty("decline_reason")
        String declineReason
) {}
