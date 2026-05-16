package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import com.bank.domain.shared.AccountNumber;
import com.bank.domain.shared.Money;
import com.bank.dto.auth.RegisterRequest;
import com.bank.dto.auth.RegisterResponse;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 10;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final GermanIbanGenerator germanIbanGenerator;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String passportNumber = request.passportNumber().trim().toUpperCase();

        if (customerRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Klient z podanym e-mailem już istnieje.");
        }

        if (customerRepository.existsByPassportNumber(passportNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Klient z podanym numerem paszportu już istnieje.");
        }

        Customer customer = customerRepository.save(Customer.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .passportNumber(passportNumber)
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(CustomerStatus.ACTIVE)
                .build());

        String accountNumber = generateUniqueGermanIban();

        accountRepository.save(Account.builder()
                .customer(customer)
                .accountNumber(AccountNumber.of(accountNumber))
                .type(AccountType.STANDARD)
                .balance(Money.zero("EUR"))
                .reservedBalance(Money.zero("EUR"))
                .status(AccountStatus.ACTIVE)
                .build());

        return new RegisterResponse(email, accountNumber);
    }

    private String generateUniqueGermanIban() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            String iban = germanIbanGenerator.generate();
            if (!accountRepository.existsByAccountNumber_Value(iban)) {
                return iban;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nie udało się wygenerować unikalnego IBAN.");
    }
}
