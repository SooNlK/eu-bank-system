package com.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfiguracja integracji z systemem płatności KLIK.
 */
@ConfigurationProperties(prefix = "app.klik")
public record KlikProperties(
        String url,
        String apiKey,
        String bankApiKey,
        String zone
) {
    public KlikProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:8000";
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "klik_mock_api_key_for_c2b";
        }
        if (bankApiKey == null || bankApiKey.isBlank()) {
            bankApiKey = "klik_mock_api_key_for_p2p";
        }
        if (zone == null || zone.isBlank()) {
            zone = "EU";
        }
    }
}
