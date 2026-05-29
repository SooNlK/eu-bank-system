package com.bank.client.cardnetwork;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardNetworkCardResponse(
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("masked_pan") String maskedPan,
        String status,
        @JsonProperty("card_type") String cardType,
        double balance,
        @JsonProperty("daily_limit") double dailyLimit,
        @JsonProperty("bank_id") String bankId
) {}
