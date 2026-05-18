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

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
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

        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Access denied");
        }

        return account;
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
                account.getCreatedAt()
        );
    }
}
