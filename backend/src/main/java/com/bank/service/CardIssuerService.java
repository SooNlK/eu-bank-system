package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import com.bank.dto.cardnetwork.IssuerAuthorizationRequest;
import com.bank.dto.cardnetwork.IssuerAuthorizationResponse;
import com.bank.dto.cardnetwork.IssuerCaptureRequest;
import com.bank.dto.cardnetwork.IssuerRefundRequest;
import com.bank.dto.cardnetwork.IssuerStatusResponse;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CardIssuerService {

    private static final String CARD_AUTH_PREFIX = "CARD_AUTH:";
    private static final String CARD_REFUND_PREFIX = "CARD_REFUND:";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public CardIssuerService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public IssuerAuthorizationResponse authorize(IssuerAuthorizationRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElse(null);

        if (account == null) {
            return declined("ACCOUNT_NOT_FOUND");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            return declined("ACCOUNT_BLOCKED");
        }

        String currency = normalizeCurrency(request.currency());
        if (!account.getBalance().getCurrency().equals(currency)) {
            return declined("CURRENCY_MISMATCH");
        }

        BigDecimal available = account.getBalance().getAmount().subtract(account.getReservedBalance().getAmount());
        if (available.compareTo(request.amount()) < 0) {
            return declined("INSUFFICIENT_FUNDS");
        }

        String authorizationCode = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Money amount = Money.of(request.amount(), currency);

        account.setReservedBalance(account.getReservedBalance().add(amount));
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(amount)
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.PENDING)
                .description(cardDescription("Autoryzacja płatności kartą", request.merchantName()))
                .referenceId(authReference(authorizationCode))
                .build());

        return new IssuerAuthorizationResponse(authorizationCode, "APPROVED", null);
    }

    @Transactional
    public IssuerStatusResponse capture(IssuerCaptureRequest request) {
        Transaction transaction = transactionRepository.findByReferenceId(authReference(request.authorizationCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autoryzacja nie istnieje."));

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            return new IssuerStatusResponse("SETTLED");
        }
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Autoryzacja nie oczekuje na capture.");
        }

        Account account = transaction.getAccount();
        Money amount = transaction.getAmount();

        if (account.getReservedBalance().getAmount().compareTo(amount.getAmount()) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brak zarezerwowanych środków dla autoryzacji.");
        }
        if (account.getBalance().getAmount().compareTo(amount.getAmount()) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            account.setReservedBalance(account.getReservedBalance().subtract(amount));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brak środków do rozliczenia transakcji.");
        }

        account.setReservedBalance(account.getReservedBalance().subtract(amount));
        account.setBalance(account.getBalance().subtract(amount));
        transaction.setStatus(TransactionStatus.COMPLETED);

        accountRepository.save(account);
        return new IssuerStatusResponse("SETTLED");
    }

    @Transactional
    public IssuerStatusResponse refund(IssuerRefundRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rachunek nie istnieje."));

        String currency = normalizeCurrency(request.currency());
        if (!account.getBalance().getCurrency().equals(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waluta zwrotu musi być zgodna z walutą rachunku.");
        }

        Money amount = Money.of(request.amount(), currency);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(amount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .description("Zwrot płatności kartą")
                .referenceId(CARD_REFUND_PREFIX + request.originalTransactionId())
                .build());

        return new IssuerStatusResponse("REFUNDED");
    }

    private IssuerAuthorizationResponse declined(String reason) {
        return new IssuerAuthorizationResponse(null, "DECLINED", reason);
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null ? "" : currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waluta musi być 3-literowym kodem ISO.");
        }
        return normalized;
    }

    private String authReference(String authorizationCode) {
        return CARD_AUTH_PREFIX + authorizationCode;
    }

    private String cardDescription(String prefix, String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            return prefix;
        }
        return prefix + " - " + merchantName.trim();
    }
}
