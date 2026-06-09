package com.bank.client.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardNetworkFullPanResponse(
        @JsonProperty("card_token")
        String cardToken,

        @JsonProperty("full_pan")
        String fullPan,

        @JsonProperty("masked_pan")
        String maskedPan,

        @JsonProperty("cvv")
        String cvv,

        @JsonProperty("expiry_month")
        int expiryMonth,

        @JsonProperty("expiry_year")
        int expiryYear
) {}
