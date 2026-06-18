package com.bank.client.klik;

import com.bank.config.KlikProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KlikClientTest {

    private KlikClient klikClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        KlikProperties properties = new KlikProperties(
                "http://localhost:8000",
                "klik_mock_api_key_for_c2b",
                "klik_mock_api_key_for_p2p",
                "EU"
        );
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        klikClient = new KlikClient(builder, properties);
    }

    @Test
    void shouldSendGenerateCodeRequest() {
        String mockResponse = """
                {
                    "code": "123456",
                    "expires_in": 120,
                    "expires_at": "2026-06-18T12:02:00Z"
                }
                """;

        this.server.expect(requestTo("http://localhost:8000/codes/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-KLIK-Api-Key", "klik_mock_api_key_for_c2b"))
                .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.notNullValue()))
                .andExpect(content().json("{\"user_id\":\"client-123\",\"zone\":\"EU\"}"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        KlikClient.CodeGenerateResponse response = klikClient.generateCode("client-123");

        assertThat(response.code()).isEqualTo("123456");
        assertThat(response.expiresIn()).isEqualTo(120);
        assertThat(response.expiresAt()).isEqualTo("2026-06-18T12:02:00Z");
    }

    @Test
    void shouldSendConfirmPaymentRequest() {
        String mockResponse = """
                {
                    "transaction_id": "tx-123",
                    "status": "COMPLETED",
                    "amount_gross": "100.00",
                    "currency": "EUR"
                }
                """;

        this.server.expect(requestTo("http://localhost:8000/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-KLIK-Api-Key", "klik_mock_api_key_for_c2b"))
                .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.notNullValue()))
                .andExpect(content().json("{\"transaction_id\":\"tx-123\",\"status\":\"ACCEPTED\"}"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        KlikClient.PaymentConfirmResponse response = klikClient.confirmPayment("tx-123", "ACCEPTED", null);

        assertThat(response.transactionId()).isEqualTo("tx-123");
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldSendRegisterAliasRequest() {
        String mockResponse = """
                {
                    "alias_id": "alias-123",
                    "phone": "+49123456789",
                    "registered_at": "2026-06-18T12:00:00Z"
                }
                """;

        this.server.expect(requestTo("http://localhost:8000/aliases/register"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-KLIK-Bank-Api-Key", "klik_mock_api_key_for_p2p"))
                .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.notNullValue()))
                .andExpect(content().json("{\"phone\":\"+49123456789\",\"account_identifier\":{\"type\":\"iban\",\"value\":\"DE12345\"},\"zone\":\"EU\"}"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        KlikClient.AliasRegisterResponse response = klikClient.registerAlias("+49123456789", "DE12345");

        assertThat(response.aliasId()).isEqualTo("alias-123");
        assertThat(response.phone()).isEqualTo("+49123456789");
    }
}
