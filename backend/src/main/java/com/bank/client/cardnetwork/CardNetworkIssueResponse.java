package com.bank.client.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardNetworkIssueResponse(
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("masked_pan") String maskedPan,
        @JsonProperty("full_pan") String fullPan,
        String cvv,
        @JsonProperty("expiry_month") int expiryMonth,
        @JsonProperty("expiry_year") int expiryYear,
        String status,
        @JsonProperty("card_type") String cardType,
        @JsonProperty("bank_id") String bankId,
        String message
) {}
