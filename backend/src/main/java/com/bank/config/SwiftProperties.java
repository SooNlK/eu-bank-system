package com.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfiguracja integracji z symulatorem sieci SWIFT (Jkwasnyy/SWIFT-Aplikacje-Biznesowe).
 */
@ConfigurationProperties(prefix = "app.swift")
public record SwiftProperties(
        String url,
        String clientId,
        String clientSecret,
        String bankBic,
        boolean enabled,
        double feePercent
) {
    public SwiftProperties {
        if (url == null || url.isBlank())           url = "http://localhost:3000";
        if (clientId == null || clientId.isBlank()) clientId = "test-client";
        if (clientSecret == null || clientSecret.isBlank()) clientSecret = "test-secret";
        if (bankBic == null || bankBic.isBlank())   bankBic = "BANKDEXX";
        if (feePercent <= 0)                        feePercent = 0.01;  // 1% domyślnie
    }
}
