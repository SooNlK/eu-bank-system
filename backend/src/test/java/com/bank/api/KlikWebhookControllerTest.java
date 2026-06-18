package com.bank.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KlikWebhookControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new KlikWebhookController()).build();
    }

    @Test
    void shouldReturnPongOnPingRequest() throws Exception {
        String pingPayload = """
                {
                    "timestamp": "2026-06-18T12:00:00Z",
                    "nonce": "test-nonce-123"
                }
                """;

        mockMvc.perform(post("/api/v1/klik/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").value("2026-06-18T12:00:00Z"))
                .andExpect(jsonPath("$.nonce").value("test-nonce-123"))
                .andExpect(jsonPath("$.pong").value(true));
    }

    @Test
    void shouldReturnAcceptedOnAuthorizeRequest() throws Exception {
        String authorizePayload = """
                {
                    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
                    "user_id": "client-123",
                    "amount": 100.50,
                    "currency": "EUR",
                    "merchant_name": "Sklep Testowy",
                    "is_on_us": false,
                    "expiry_time": "2026-06-18T12:05:00Z",
                    "zone": "EU"
                }
                """;

        mockMvc.perform(post("/api/v1/klik/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authorizePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.will_prompt_user").value(true));
    }
}
