package com.bank.bootstrap;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import com.bank.domain.shared.AccountNumber;
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
                .passwordHash(passwordEncoder.encode("password123"))
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer erika = Customer.builder()
                .firstName("Erika")
                .lastName("Schmidt")
                .email("erika.schmidt@example.de")
                .passwordHash(passwordEncoder.encode("password123"))
                .status(CustomerStatus.ACTIVE)
                .build();

        customerRepository.saveAll(List.of(hans, erika));

        // 2. Create Accounts for Hans (DE IBANs, EUR currency)
        Account hansAccount1 = Account.builder()
                .customer(hans)
                .accountNumber(AccountNumber.of("DE89100110010123456789"))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("2500.00"), "EUR"))
                .reservedBalance(Money.of(BigDecimal.ZERO, "EUR"))
                .status(AccountStatus.ACTIVE)
                .build();

        Account hansAccount2 = Account.builder()
                .customer(hans)
                .accountNumber(AccountNumber.of("DE89100110010987654321"))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("15420.75"), "EUR"))
                .reservedBalance(Money.of(BigDecimal.ZERO, "EUR"))
                .status(AccountStatus.ACTIVE)
                .build();

        // 3. Create Account for Erika
        Account erikaAccount1 = Account.builder()
                .customer(erika)
                .accountNumber(AccountNumber.of("DE50200300400556677889"))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("3200.50"), "EUR"))
                .reservedBalance(Money.of(BigDecimal.ZERO, "EUR"))
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.saveAll(List.of(hansAccount1, hansAccount2, erikaAccount1));

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

        // 5. Create Cards
        com.bank.domain.card.Card hansCard = com.bank.domain.card.Card.builder()
                .account(hansAccount1)
                .last4("8822")
                .type(com.bank.domain.card.CardType.DEBIT)
                .status(com.bank.domain.card.CardStatus.ACTIVE)
                .expiresAt(LocalDate.now().plusYears(4))
                .dailyLimit(Money.of(new BigDecimal("2000.00"), "EUR"))
                .monthlyLimit(Money.of(new BigDecimal("10000.00"), "EUR"))
                .build();

        cardRepository.save(hansCard);

        // 6. Create SEPA Transfer
        com.bank.domain.transfer.Transfer transfer = com.bank.domain.transfer.Transfer.builder()
                .fromAccount(hansAccount1)
                .toAccount(erikaAccount1)
                .amount(Money.of(new BigDecimal("150.00"), "EUR"))
                .channel(com.bank.domain.transfer.TransferChannel.SEPA)
                .status(com.bank.domain.transfer.TransferStatus.COMPLETED)
                .description("Miete Anteil (Rent share)")
                .completedAt(LocalDateTime.now())
                .build();

        transferRepository.save(transfer);

        log.info("European data seeding completed successfully.");
    }
}
