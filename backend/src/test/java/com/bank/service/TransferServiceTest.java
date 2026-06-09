package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import com.bank.domain.shared.AccountNumber;
import com.bank.domain.shared.Money;
import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.transfer.TransferStatus;
import com.bank.dto.transfer.TransferRequest;
import com.bank.dto.transfer.TransferResponse;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GermanIbanGenerator ibanGenerator;

    @Test
    void shouldCompleteInternalTransferImmediately() {
        Customer owner = createCustomer("owner@example.test");
        Customer receiver = createCustomer("receiver@example.test");
        Account fromAccount = createAccount(owner, new BigDecimal("500.00"));
        Account toAccount = createAccount(receiver, new BigDecimal("100.00"));

        TransferResponse response = transferService.execute(new TransferRequest(
                fromAccount.getId(),
                toAccount.getAccountNumber().getValue(),
                new BigDecimal("125.50"),
                "eur",
                LocalDate.now(),
                TransferChannel.INTERNAL,
                "Internal top-up",
                null,
                null,
                null,
                null
        ), owner.getEmail());

        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.valueDate()).isEqualTo(LocalDate.now());
        assertThat(accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance().getAmount())
                .isEqualByComparingTo("374.50");
        assertThat(accountRepository.findById(toAccount.getId()).orElseThrow().getBalance().getAmount())
                .isEqualByComparingTo("225.50");
        assertThat(transactionRepository.findAll())
                .filteredOn(transaction -> response.id().toString().equals(transaction.getReferenceId()))
                .hasSize(2);
    }

    @Test
    void shouldFailTransferWhenBalanceIsTooLow() {
        Customer owner = createCustomer("low-balance@example.test");
        Customer receiver = createCustomer("low-balance-receiver@example.test");
        Account fromAccount = createAccount(owner, new BigDecimal("10.00"));
        Account toAccount = createAccount(receiver, new BigDecimal("100.00"));

        TransferResponse response = transferService.execute(new TransferRequest(
                fromAccount.getId(),
                toAccount.getAccountNumber().getValue(),
                new BigDecimal("99.00"),
                "EUR",
                LocalDate.now(),
                TransferChannel.INTERNAL,
                "Too large",
                null,
                null,
                null,
                null
        ), owner.getEmail());

        assertThat(response.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance().getAmount())
                .isEqualByComparingTo("10.00");
        assertThat(accountRepository.findById(toAccount.getId()).orElseThrow().getBalance().getAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectInvalidIbanBeforeCreatingTransfer() {
        Customer owner = createCustomer("invalid-iban@example.test");
        Account fromAccount = createAccount(owner, new BigDecimal("500.00"));
        long transferCount = transferRepository.count();

        assertThatThrownBy(() -> transferService.execute(new TransferRequest(
                fromAccount.getId(),
                "DE001234",
                new BigDecimal("25.00"),
                "EUR",
                LocalDate.now(),
                TransferChannel.INTERNAL,
                "Bad IBAN",
                null,
                null,
                null,
                null
        ), owner.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(transferRepository.count()).isEqualTo(transferCount);
    }

    @Test
    void shouldValidateInternalTransferValueDate() {
        Customer owner = createCustomer("future-date@example.test");
        Customer receiver = createCustomer("future-date-receiver@example.test");
        Account fromAccount = createAccount(owner, new BigDecimal("500.00"));
        Account toAccount = createAccount(receiver, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.execute(new TransferRequest(
                fromAccount.getId(),
                toAccount.getAccountNumber().getValue(),
                new BigDecimal("25.00"),
                "EUR",
                LocalDate.now().plusDays(1),
                TransferChannel.INTERNAL,
                "Future value date",
                null,
                null,
                null,
                null
        ), owner.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Customer createCustomer(String email) {
        return customerRepository.save(Customer.builder()
                .firstName("Test")
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
