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
        BigDecimal initialLiquidity,
        String webhookSecret,
        String webhookUrl
) {
    public EuPaymentsProperties {
        if (targetUrl == null)     targetUrl = "http://localhost:8001";
        if (sepaBatchUrl == null)  sepaBatchUrl = "http://localhost:8002";
        if (sepaInstantUrl == null) sepaInstantUrl = "http://localhost:8003";
        if (bankBic == null)       bankBic = "BANKDEXX";
        if (bankName == null)      bankName = "Deutsche Bank";
        if (initialLiquidity == null) initialLiquidity = new BigDecimal("1000000.00");
        if (webhookSecret == null) webhookSecret = "my-secret-key";
        if (webhookUrl == null || webhookUrl.isBlank()) {
            String host = null;
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                // Ignore
            }
            if (host == null || host.isBlank()) {
                host = "eu-bank-backend";
            }
            webhookUrl = "http://" + host + ":8080/api/v1/target-settlement";
        }
    }
}
