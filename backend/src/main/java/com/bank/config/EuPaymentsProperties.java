package com.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.eu-payments")
public record EuPaymentsProperties(
        String targetUrl,
        String sepaBatchUrl,
        String sepaInstantUrl,
        String bankBic,
        String bankName,
        BigDecimal initialLiquidity
) {
    public EuPaymentsProperties {
        if (targetUrl == null)     targetUrl = "http://localhost:8001";
        if (sepaBatchUrl == null)  sepaBatchUrl = "http://localhost:8002";
        if (sepaInstantUrl == null) sepaInstantUrl = "http://localhost:8003";
        if (bankBic == null)       bankBic = "BANKDEXX";
        if (bankName == null)      bankName = "Deutsche Bank";
        if (initialLiquidity == null) initialLiquidity = new BigDecimal("1000000.00");
    }
}
