package com.bank.api;

import com.bank.service.KlikService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KlikWebhookControllerTest {

    private MockMvc mockMvc;
    private KlikService klikService;

    @BeforeEach
    void setUp() {
        klikService = mock(KlikService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new KlikWebhookController(klikService)).build();
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
                    "user_id": "550e8400-e29b-41d4-a716-446655441111",
                    "amount": 100.50,
                    "currency": "EUR",
                    "merchant_name": "Sklep Testowy",
                    "is_on_us": false,
                    "expiry_time": "2026-06-18T12:05:00Z",
                    "zone": "EU"
                }
                """;

        when(klikService.authorizeWebhook(any())).thenReturn(
                new KlikWebhookController.AuthorizeResponse(true, true)
        );

        mockMvc.perform(post("/api/v1/klik/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authorizePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.will_prompt_user").value(true));
    }
}
