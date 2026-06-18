package com.bank.api;

import com.bank.dto.blik.BlikConfirmRequest;
import com.bank.dto.blik.BlikGenerateRequest;
import com.bank.dto.blik.BlikGenerateResponse;
import com.bank.domain.klik.KlikTransaction;
import com.bank.domain.account.Account;
import com.bank.domain.shared.AccountNumber;
import com.bank.service.KlikService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlikControllerTest {

    private MockMvc mockMvc;
    private KlikService klikService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        klikService = mock(KlikService.class);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); 
        
        mockMvc = MockMvcBuilders.standaloneSetup(new BlikController(klikService)).build();
    }

    @Test
    void shouldGenerateCode() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID localCodeId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(120);

        when(klikService.generateCode(any(BlikGenerateRequest.class), eq("hans@example.test")))
                .thenReturn(new BlikGenerateResponse(localCodeId, "123456", expiresAt));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("hans@example.test", "password");

        String payload = objectMapper.writeValueAsString(new BlikGenerateRequest(accountId));

        mockMvc.perform(post("/api/blik/generate")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(localCodeId.toString()))
                .andExpect(jsonPath("$.code").value("123456"));
    }

    @Test
    void shouldGetPendingTransactions() throws Exception {
        UUID transactionId = UUID.randomUUID();
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(UUID.randomUUID());
        when(account.getAccountNumber()).thenReturn(AccountNumber.of("DE12345"));

        KlikTransaction tx = KlikTransaction.builder()
                .id(transactionId)
                .account(account)
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .merchantName("Zabka")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(klikService.getPendingTransactions("hans@example.test"))
                .thenReturn(Collections.singletonList(tx));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("hans@example.test", "password");

        mockMvc.perform(get("/api/blik/pending")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$[0].merchantName").value("Zabka"))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }

    @Test
    void shouldConfirmTransaction() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("hans@example.test", "password");

        String payload = objectMapper.writeValueAsString(new BlikConfirmRequest(transactionId, "ACCEPTED"));

        mockMvc.perform(post("/api/blik/confirm")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(klikService).confirmTransaction(transactionId, "ACCEPTED", "hans@example.test");
    }
}
