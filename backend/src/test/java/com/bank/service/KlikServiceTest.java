package com.bank.service;

import com.bank.client.klik.KlikClient;
import com.bank.domain.account.Account;
import com.bank.domain.customer.Customer;
import com.bank.domain.klik.KlikCode;
import com.bank.domain.klik.KlikTransaction;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.Transaction;
import com.bank.dto.blik.BlikGenerateRequest;
import com.bank.dto.blik.BlikGenerateResponse;
import com.bank.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.bank.dto.blik.BlikP2pTransferRequest;
import com.bank.dto.transfer.TransferResponse;
import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.shared.AccountNumber;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KlikServiceTest {

    private KlikClient klikClient;
    private KlikCodeRepository klikCodeRepository;
    private KlikTransactionRepository klikTransactionRepository;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private CustomerRepository customerRepository;
    private AccountService accountService;
    private TransferService transferService;

    private KlikService klikService;

    @BeforeEach
    void setUp() {
        klikClient = mock(KlikClient.class);
        klikCodeRepository = mock(KlikCodeRepository.class);
        klikTransactionRepository = mock(KlikTransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        customerRepository = mock(CustomerRepository.class);
        accountService = mock(AccountService.class);
        transferService = mock(TransferService.class);

        klikService = new KlikService(
                klikClient,
                klikCodeRepository,
                klikTransactionRepository,
                accountRepository,
                transactionRepository,
                customerRepository,
                accountService,
                transferService
        );
    }

    @Test
    void shouldGenerateCodeSuccessfully() {
        UUID accountId = UUID.randomUUID();
        String email = "hans@example.test";
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);

        when(accountService.getAccountEntity(accountId, email)).thenReturn(account);
        when(klikClient.generateCode(accountId.toString())).thenReturn(
                new KlikClient.CodeGenerateResponse("123456", 120, "2026-06-18T13:00:00Z")
        );

        KlikCode savedCode = KlikCode.builder()
                .id(UUID.randomUUID())
                .account(account)
                .codeHash("hash")
                .expiresAt(LocalDateTime.now())
                .build();
        when(klikCodeRepository.save(any(KlikCode.class))).thenReturn(savedCode);

        BlikGenerateResponse response = klikService.generateCode(new BlikGenerateRequest(accountId), email);

        assertThat(response.code()).isEqualTo("123456");
        verify(klikCodeRepository).save(any(KlikCode.class));
    }

    @Test
    void shouldConfirmAcceptedPaymentSuccessfully() {
        UUID transactionId = UUID.randomUUID();
        String email = "hans@example.test";
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(UUID.randomUUID());
        Account account = mock(Account.class);
        when(account.getCustomer()).thenReturn(customer);
        when(account.getBalance()).thenReturn(Money.of(new BigDecimal("500.00"), "EUR"));
        when(account.getReservedBalance()).thenReturn(Money.zero("EUR"));

        KlikTransaction klikTx = KlikTransaction.builder()
                .id(transactionId)
                .account(account)
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .merchantName("Zabka")
                .status("PENDING")
                .build();

        when(klikTransactionRepository.findById(transactionId)).thenReturn(Optional.of(klikTx));
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
        when(klikClient.confirmPayment(anyString(), eq("ACCEPTED"), any())).thenReturn(
                new KlikClient.PaymentConfirmResponse(transactionId.toString(), "COMPLETED", "100.00", "1.00", "0.50", "98.50", "EUR", null, "2026-06-18T13:00:00Z")
        );

        klikService.confirmTransaction(transactionId, "ACCEPTED", email);

        verify(account, times(2)).setReservedBalance(any());
        verify(account).setBalance(any());
        verify(klikTransactionRepository, times(2)).save(klikTx);
        assertThat(klikTx.getStatus()).isEqualTo("COMPLETED");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldDeclinePaymentIfInsufficientFunds() {
        UUID transactionId = UUID.randomUUID();
        String email = "hans@example.test";
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(UUID.randomUUID());
        Account account = mock(Account.class);
        when(account.getCustomer()).thenReturn(customer);
        when(account.getBalance()).thenReturn(Money.of(new BigDecimal("50.00"), "EUR"));
        when(account.getReservedBalance()).thenReturn(Money.zero("EUR"));

        KlikTransaction klikTx = KlikTransaction.builder()
                .id(transactionId)
                .account(account)
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .merchantName("Zabka")
                .status("PENDING")
                .build();

        when(klikTransactionRepository.findById(transactionId)).thenReturn(Optional.of(klikTx));
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> klikService.confirmTransaction(transactionId, "ACCEPTED", email))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Niewystarczające środki");

        assertThat(klikTx.getStatus()).isEqualTo("REJECTED");
        assertThat(klikTx.getRejectReason()).isEqualTo("INSUFFICIENT_FUNDS");
        verify(klikClient).confirmPayment(transactionId.toString(), "REJECTED", "INSUFFICIENT_FUNDS");
    }

    @Test
    void shouldRegisterAliasSuccessfully() {
        UUID accountId = UUID.randomUUID();
        String email = "hans@example.test";
        String phone = "+48123456789";
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(AccountNumber.of("PL123"));
        when(accountService.getAccountEntity(accountId, email)).thenReturn(account);

        when(klikClient.registerAlias(phone, "PL123")).thenReturn(
                new KlikClient.AliasRegisterResponse("alias-123", phone, "2026-06-18T13:00:00Z")
        );

        KlikClient.AliasRegisterResponse response = klikService.registerAlias(accountId, phone, email);

        assertThat(response.phone()).isEqualTo(phone);
        assertThat(response.aliasId()).isEqualTo("alias-123");
        verify(klikClient).registerAlias(phone, "PL123");
    }

    @Test
    void shouldDeleteAliasSuccessfully() {
        String phone = "+48123456789";
        String email = "hans@example.test";
        Customer customer = mock(Customer.class);
        when(customer.getEmail()).thenReturn(email);
        Account account = mock(Account.class);
        when(account.getCustomer()).thenReturn(customer);

        when(klikClient.lookupAlias(phone)).thenReturn(
                new KlikClient.AliasLookupResponse(phone, "bank-1", "code-1", new KlikClient.AccountIdentifier("iban", "PL123"))
        );
        when(accountRepository.findByAccountNumber_Value("PL123")).thenReturn(Optional.of(account));

        klikService.deleteAlias(phone, email);

        verify(klikClient).deleteAlias(phone);
    }

    @Test
    void shouldExecuteP2pTransferSuccessfully() {
        UUID fromAccountId = UUID.randomUUID();
        String email = "hans@example.test";
        String phone = "+48987654321";
        BigDecimal amount = new BigDecimal("50.00");
        BlikP2pTransferRequest request = new BlikP2pTransferRequest(fromAccountId, phone, amount, "EUR", "For lunch");

        Account fromAccount = mock(Account.class);
        when(accountService.getAccountEntity(fromAccountId, email)).thenReturn(fromAccount);

        when(klikClient.lookupAlias(phone)).thenReturn(
                new KlikClient.AliasLookupResponse(phone, "bank-2", "code-2", new KlikClient.AccountIdentifier("iban", "PL999"))
        );
        when(transferService.isAccountOurs("PL999")).thenReturn(false);

        TransferResponse expectedResponse = mock(TransferResponse.class);
        when(transferService.execute(any(), eq(email))).thenReturn(expectedResponse);

        TransferResponse actualResponse = klikService.p2pTransfer(request, email);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(transferService).execute(argThat(tr -> 
                tr.fromAccountId().equals(fromAccountId) &&
                tr.toIban().equals("PL999") &&
                tr.amount().equals(amount) &&
                tr.channel() == TransferChannel.SEPA_INSTANT &&
                tr.description().equals("For lunch")
        ), eq(email));
    }
}
