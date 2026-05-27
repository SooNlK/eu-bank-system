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
