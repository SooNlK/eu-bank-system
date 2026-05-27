package com.bank.bootstrap;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import com.bank.domain.shared.AccountNumber;
import com.bank.domain.shared.IBAN;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import com.bank.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Database already seeded. Skipping...");
            return;
        }

        log.info("Seeding initial data for European Bank System (Germany)...");

        // 1. Create Customers (German names)
        Customer hans = Customer.builder()
                .firstName("Hans")
                .lastName("Müller")
                .email("hans.mueller@example.de")
                .passportNumber("C4F7X2L90")
                .passwordHash(passwordEncoder.encode("password123"))
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer erika = Customer.builder()
                .firstName("Erika")
                .lastName("Schmidt")
                .email("erika.schmidt@example.de")
                .passportNumber("C9K1P8M44")
                .passwordHash(passwordEncoder.encode("password123"))
                .status(CustomerStatus.ACTIVE)
                .build();

        customerRepository.saveAll(List.of(hans, erika));

        // 2. Create Accounts for Hans (DE IBANs, EUR currency)
        Account hansAccount1 = Account.builder()
                .customer(hans)
                .accountNumber(validAccountNumber("DE89370400440532013000"))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("2500.00"), "EUR"))
                .reservedBalance(Money.of(BigDecimal.ZERO, "EUR"))
                .status(AccountStatus.ACTIVE)
                .build();

        // 3. Create Account for Erika
        Account erikaAccount1 = Account.builder()
                .customer(erika)
                .accountNumber(validAccountNumber("DE12500105170648489890"))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("3200.50"), "EUR"))
                .reservedBalance(Money.of(BigDecimal.ZERO, "EUR"))
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.saveAll(List.of(hansAccount1, erikaAccount1));

        // 4. Create Transactions for Hans
        Transaction t1 = Transaction.builder()
                .account(hansAccount1)
                .amount(Money.of(new BigDecimal("2500.00"), "EUR"))
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .description("Gehalt (Salary) - TechCorp GmbH")
                .build();

        Transaction t2 = Transaction.builder()
                .account(hansAccount1)
                .amount(Money.of(new BigDecimal("45.20"), "EUR"))
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .description("REWE Supermarket Berlin")
                .build();

        Transaction t3 = Transaction.builder()
                .account(hansAccount1)
                .amount(Money.of(new BigDecimal("12.50"), "EUR"))
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .description("Bäckerei Hansen")
                .build();

        transactionRepository.saveAll(List.of(t1, t2, t3));

        // 6. Create SEPA Transfer
        com.bank.domain.transfer.Transfer transfer = com.bank.domain.transfer.Transfer.builder()
                .fromAccount(hansAccount1)
                .toAccount(erikaAccount1)
                .amount(Money.of(new BigDecimal("150.00"), "EUR"))
                .channel(com.bank.domain.transfer.TransferChannel.SEPA)
                .status(com.bank.domain.transfer.TransferStatus.COMPLETED)
                .description("Miete Anteil (Rent share)")
                .valueDate(LocalDate.now())
                .completedAt(LocalDateTime.now())
                .build();

        transferRepository.save(transfer);

        log.info("European data seeding completed successfully.");
    }

    private AccountNumber validAccountNumber(String iban) {
        return AccountNumber.of(IBAN.of(iban).getValue());
    }
}
