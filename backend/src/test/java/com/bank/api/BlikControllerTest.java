package com.bank.api;

import com.bank.dto.blik.BlikConfirmRequest;
import com.bank.dto.blik.BlikGenerateRequest;
import com.bank.dto.blik.BlikGenerateResponse;
import com.bank.dto.blik.BlikRegisterAliasRequest;
import com.bank.dto.blik.BlikP2pTransferRequest;
import com.bank.dto.transfer.TransferResponse;
import com.bank.client.klik.KlikClient;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void shouldRegisterAlias() throws Exception {
        UUID accountId = UUID.randomUUID();
        String phone = "+48123456789";
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("hans@example.test", "password");

        KlikClient.AliasRegisterResponse registerResp = new KlikClient.AliasRegisterResponse("alias-123", phone, "2026-06-18T13:00:00Z");
        when(klikService.registerAlias(eq(accountId), eq(phone), eq("hans@example.test")))
                .thenReturn(registerResp);

        String payload = objectMapper.writeValueAsString(new BlikRegisterAliasRequest(accountId, phone));

        mockMvc.perform(post("/api/blik/p2p/register")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias_id").value("alias-123"))
                .andExpect(jsonPath("$.phone").value(phone));
    }

    @Test
    void shouldUnregisterAlias() throws Exception {
        String phone = "+48123456789";
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("hans@example.test", "password");

        mockMvc.perform(delete("/api/blik/p2p/unregister")
                        .principal(principal)
                        .param("phone", phone))
                .andExpect(status().isNoContent());

        verify(klikService).deleteAlias(phone, "hans@example.test");
    }

    @Test
    void shouldP2pTransfer() throws Exception {
        UUID fromAccountId = UUID.randomUUID();
        String toPhone = "+48987654321";
        BigDecimal amount = new BigDecimal("50.00");
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("hans@example.test", "password");

        TransferResponse transferResp = new TransferResponse(
                UUID.randomUUID(), fromAccountId, null, "PL999", null, "Odbiorca KLIK P2P",
                amount, "EUR", com.bank.domain.transfer.TransferChannel.SEPA_INSTANT,
                com.bank.domain.transfer.TransferStatus.COMPLETED, "For pizza", java.time.LocalDate.now(),
                false, LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null
        );

        BlikP2pTransferRequest request = new BlikP2pTransferRequest(fromAccountId, toPhone, amount, "EUR", "For pizza");
        when(klikService.p2pTransfer(any(BlikP2pTransferRequest.class), eq("hans@example.test")))
                .thenReturn(transferResp);

        String payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/blik/p2p/transfer")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toIban").value("PL999"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.channel").value("SEPA_INSTANT"));
    }
}
