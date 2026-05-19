package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.customer.Customer;
import com.bank.dto.account.AccountResponse;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final GermanIbanGenerator germanIbanGenerator;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          GermanIbanGenerator germanIbanGenerator) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.germanIbanGenerator = germanIbanGenerator;
    }

    public List<AccountResponse> getAccountsForCurrentUser(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return accountRepository.findByCustomerId(customer.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(UUID accountId, String email) {
        Account account = getAccountEntity(accountId, email);
        return mapToResponse(account);
    }

    public AccountResponse getBalance(UUID accountId, String email) {
        return getAccount(accountId, email);
    }

    public Account getAccountEntity(UUID accountId, String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // If it's a junior account, parent also has access to it!
        if (!account.getCustomer().getId().equals(customer.getId())) {
            boolean isParentOfChild = account.getType() == com.bank.domain.account.AccountType.JUNIOR &&
                    account.getParentAccount() != null &&
                    account.getParentAccount().getCustomer().getId().equals(customer.getId());
            if (!isParentOfChild) {
                throw new RuntimeException("Access denied");
            }
        }

        return account;
    }

    @Transactional
    public AccountResponse registerJuniorAccount(com.bank.dto.account.RegisterJuniorRequest request, String parentEmail) {
        Customer parent = customerRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Rodzic nie został znaleziony."));

        Account parentAccount = accountRepository.findById(request.parentAccountId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Rachunek rodzica nie istnieje."));

        if (!parentAccount.getCustomer().getId().equals(parent.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Brak dostępu do podanego rachunku rodzica.");
        }

        java.time.LocalDate dateOfBirth = request.dateOfBirth();
        int age = java.time.Period.between(dateOfBirth, java.time.LocalDate.now()).getYears();
        if (age < 7 || age > 13) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Wiek dziecka musi wynosić od 7 do 13 lat.");
        }

        String childEmail = request.email().trim().toLowerCase();
        if (customerRepository.existsByEmail(childEmail)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Klient z podanym e-mailem już istnieje.");
        }

        String passportNumber = request.passportNumber().trim().toUpperCase();
        if (customerRepository.existsByPassportNumber(passportNumber)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Klient z podanym numerem paszportu/legitymacji już istnieje.");
        }

        Customer child = customerRepository.save(Customer.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(childEmail)
                .passportNumber(passportNumber)
                .dateOfBirth(dateOfBirth)
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(com.bank.domain.customer.CustomerStatus.ACTIVE)
                .build());

        String childIban = generateUniqueGermanIban();

        Account childAccount = accountRepository.save(Account.builder()
                .customer(child)
                .parentAccount(parentAccount)
                .accountNumber(com.bank.domain.shared.AccountNumber.of(childIban))
                .type(com.bank.domain.account.AccountType.JUNIOR)
                .balance(com.bank.domain.shared.Money.zero("EUR"))
                .reservedBalance(com.bank.domain.shared.Money.zero("EUR"))
                .status(com.bank.domain.account.AccountStatus.ACTIVE)
                .build());

        return mapToResponse(childAccount);
    }

    public List<AccountResponse> getJuniorAccountsForParent(String parentEmail) {
        Customer parent = customerRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Rodzic nie został znaleziony."));

        return accountRepository.findAll().stream()
                .filter(acc -> acc.getType() == com.bank.domain.account.AccountType.JUNIOR &&
                        acc.getParentAccount() != null &&
                        acc.getParentAccount().getCustomer().getId().equals(parent.getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueGermanIban() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String iban = germanIbanGenerator.generate();
            if (!accountRepository.existsByAccountNumber_Value(iban)) {
                return iban;
            }
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Nie udało się wygenerować unikalnego IBAN dla dziecka.");
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber().getValue(),
                account.getType(),
                account.getBalance().getAmount(),
                account.getReservedBalance().getAmount(),
                account.getBalance().getCurrency(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getParentAccount() == null ? null : account.getParentAccount().getId(),
                account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName()
        );
    }
}
