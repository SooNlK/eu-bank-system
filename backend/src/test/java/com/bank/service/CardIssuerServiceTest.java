package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import com.bank.domain.shared.AccountNumber;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.dto.cardnetwork.IssuerAuthorizationRequest;
import com.bank.dto.cardnetwork.IssuerAuthorizationResponse;
import com.bank.dto.cardnetwork.IssuerCaptureRequest;
import com.bank.dto.cardnetwork.IssuerRefundRequest;
import com.bank.repository.AccountRepository;
import com.bank.repository.CardRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CardIssuerServiceTest {

    @Autowired
    private CardIssuerService cardIssuerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private GermanIbanGenerator ibanGenerator;

    @Test
    void shouldReserveFundsAndCaptureCardPayment() {
        Customer customer = createCustomer("card-auth@example.test");
        Account account = createAccount(customer, new BigDecimal("500.00"));
        UUID networkTransactionId = UUID.randomUUID();

        IssuerAuthorizationResponse authorization = cardIssuerService.authorize(new IssuerAuthorizationRequest(
                account.getId(),
                new BigDecimal("125.50"),
                "eur",
                networkTransactionId,
                "Test Merchant"
        ));

        assertThat(authorization.status()).isEqualTo("APPROVED");
        assertThat(authorization.authorizationCode()).startsWith("AUTH-");
        Account reservedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reservedAccount.getBalance().getAmount()).isEqualByComparingTo("500.00");
        assertThat(reservedAccount.getReservedBalance().getAmount()).isEqualByComparingTo("125.50");

        cardIssuerService.capture(new IssuerCaptureRequest(authorization.authorizationCode(), networkTransactionId));

        Account capturedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(capturedAccount.getBalance().getAmount()).isEqualByComparingTo("374.50");
        assertThat(capturedAccount.getReservedBalance().getAmount()).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.findByReferenceId("CARD_AUTH:" + authorization.authorizationCode()))
                .get()
                .extracting(transaction -> transaction.getStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldDeclineWhenAvailableBalanceIsTooLow() {
        Customer customer = createCustomer("card-low@example.test");
        Account account = createAccount(customer, new BigDecimal("10.00"));

        IssuerAuthorizationResponse authorization = cardIssuerService.authorize(new IssuerAuthorizationRequest(
                account.getId(),
                new BigDecimal("99.00"),
                "EUR",
                UUID.randomUUID(),
                "Test Merchant"
        ));

        assertThat(authorization.status()).isEqualTo("DECLINED");
        assertThat(authorization.declineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(transactionRepository.findAll())
                .noneMatch(transaction -> transaction.getReferenceId() != null
                        && transaction.getReferenceId().startsWith("CARD_AUTH:"));
    }

    @Test
    void shouldRefundCardPayment() {
        Customer customer = createCustomer("card-refund@example.test");
        Account account = createAccount(customer, new BigDecimal("100.00"));

        cardIssuerService.refund(new IssuerRefundRequest(
                account.getId(),
                new BigDecimal("25.00"),
                "EUR",
                UUID.randomUUID()
        ));

        Account refundedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(refundedAccount.getBalance().getAmount()).isEqualByComparingTo("125.00");
    }

    @Test
    void shouldDirectCaptureCardPaymentWithoutPriorAuthorization() {
        Customer customer = createCustomer("card-direct-capture@example.test");
        Account account = createAccount(customer, new BigDecimal("300.00"));
        String cardToken = "token-direct-capture-test";

        // Save card to DB
        cardRepository.save(com.bank.domain.card.Card.builder()
                .account(account)
                .externalCardToken(cardToken)
                .maskedPan("4100 01** **** 1296")
                .last4("1296")
                .type(com.bank.domain.card.CardType.VIRTUAL)
                .status(com.bank.domain.card.CardStatus.ACTIVE)
                .expiresAt(java.time.LocalDate.now().plusYears(3))
                .dailyLimit(Money.of(new BigDecimal("1000.00"), "EUR"))
                .monthlyLimit(Money.of(new BigDecimal("5000.00"), "EUR"))
                .build());

        UUID networkTransactionId = UUID.randomUUID();
        String authorizationCode = "AUTH-DIRECT-123";

        // Call capture directly without prior authorize() call
        cardIssuerService.capture(new IssuerCaptureRequest(
                authorizationCode,
                networkTransactionId,
                new BigDecimal("75.20"),
                "EUR",
                "MERCH-POS-1",
                cardToken
        ));

        Account capturedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(capturedAccount.getBalance().getAmount()).isEqualByComparingTo("224.80");
        assertThat(capturedAccount.getReservedBalance().getAmount()).isEqualByComparingTo("0.00");

        // Verify transaction exists with status COMPLETED
        assertThat(transactionRepository.findByReferenceId("CARD_AUTH:" + authorizationCode))
                .get()
                .extracting(transaction -> transaction.getStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldFailDirectCaptureWhenDailyLimitIsExceeded() {
        Customer customer = createCustomer("card-limit-fail@example.test");
        Account account = createAccount(customer, new BigDecimal("2000.00"));
        String cardToken = "token-limit-fail-test";

        cardRepository.save(com.bank.domain.card.Card.builder()
                .account(account)
                .externalCardToken(cardToken)
                .maskedPan("4100 01** **** 1296")
                .last4("1296")
                .type(com.bank.domain.card.CardType.VIRTUAL)
                .status(com.bank.domain.card.CardStatus.ACTIVE)
                .expiresAt(java.time.LocalDate.now().plusYears(3))
                .dailyLimit(Money.of(new BigDecimal("500.00"), "EUR"))
                .monthlyLimit(Money.of(new BigDecimal("5000.00"), "EUR"))
                .build());

        UUID networkTransactionId1 = UUID.randomUUID();
        cardIssuerService.capture(new IssuerCaptureRequest(
                "AUTH-LIMIT-1",
                networkTransactionId1,
                new BigDecimal("400.00"),
                "EUR",
                "MERCH-POS-1",
                cardToken
        ));

        UUID networkTransactionId2 = UUID.randomUUID();
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            cardIssuerService.capture(new IssuerCaptureRequest(
                    "AUTH-LIMIT-2",
                    networkTransactionId2,
                    new BigDecimal("200.00"),
                    "EUR",
                    "MERCH-POS-1",
                    cardToken
            ));
        });

        Account finalAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(finalAccount.getBalance().getAmount()).isEqualByComparingTo("1600.00");
    }

    private Customer createCustomer(String email) {
        return customerRepository.save(Customer.builder()
                .firstName("Card")
                .lastName("Customer")
                .email(email)
                .passportNumber(email.substring(0, Math.min(20, email.length())).replace("@", ""))
                .passwordHash("hash")
                .status(CustomerStatus.ACTIVE)
                .build());
    }

    private Account createAccount(Customer customer, BigDecimal balance) {
        return accountRepository.save(Account.builder()
                .customer(customer)
                .accountNumber(AccountNumber.of(ibanGenerator.generate()))
                .type(AccountType.STANDARD)
                .balance(Money.of(balance, "EUR"))
                .reservedBalance(Money.zero("EUR"))
                .status(AccountStatus.ACTIVE)
                .build());
    }
}
