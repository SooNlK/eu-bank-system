package com.bank.client.cardnetwork;

public record CardNetworkStatusResponse(
        boolean success,
        String message
) {}
