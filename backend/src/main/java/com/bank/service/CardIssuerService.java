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
import com.bank.domain.card.Card;
import com.bank.repository.AccountRepository;
import com.bank.repository.CardRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CardIssuerService {

    private static final String CARD_AUTH_PREFIX = "CARD_AUTH:";
    private static final String CARD_REFUND_PREFIX = "CARD_REFUND:";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final FxService fxService;
    private final TransactionService transactionService;

    public CardIssuerService(AccountRepository accountRepository,
                             TransactionRepository transactionRepository,
                             CardRepository cardRepository,
                             FxService fxService,
                             TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.fxService = fxService;
        this.transactionService = transactionService;
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
        String accountCurrency = account.getBalance().getCurrency();

        BigDecimal convertedAmountVal;
        try {
            if (accountCurrency.equals(currency)) {
                convertedAmountVal = request.amount();
            } else {
                convertedAmountVal = fxService.convert(request.amount(), currency, accountCurrency);
            }
        } catch (Exception e) {
            return declined("CURRENCY_MISMATCH");
        }

        BigDecimal available = account.getBalance().getAmount().subtract(account.getReservedBalance().getAmount());
        if (available.compareTo(convertedAmountVal) < 0) {
            return declined("INSUFFICIENT_FUNDS");
        }

        String authorizationCode = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Money amount = Money.of(convertedAmountVal, accountCurrency);

        account.setReservedBalance(account.getReservedBalance().add(amount));
        accountRepository.save(account);

        String txDesc = "Autoryzacja płatności kartą";
        if (!accountCurrency.equals(currency)) {
            txDesc += " (" + request.amount() + " " + currency + ")";
        }

        transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(amount)
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.PENDING)
                .description(cardDescription(txDesc, request.merchantName()))
                .referenceId(authReference(authorizationCode))
                .build());

        return new IssuerAuthorizationResponse(authorizationCode, "APPROVED", null);
    }

    @Transactional
    public IssuerStatusResponse capture(IssuerCaptureRequest request) {
        Transaction transaction = transactionRepository.findByReferenceId(authReference(request.authorizationCode()))
                .orElse(null);

        if (transaction != null) {
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
        } else {
            // Direct Capture Flow (offline / POS payment without prior authorization)
            if (request.cardToken() == null || request.amount() == null || request.currency() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Autoryzacja nie istnieje i brakuje danych do obciążenia bezpośredniego.");
            }

            Card card = cardRepository.findByExternalCardToken(request.cardToken())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Karta nie istnieje w banku."));

            Account account = card.getAccount();
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Rachunek powiązany z kartą jest zablokowany.");
            }

            String currency = normalizeCurrency(request.currency());
            String accountCurrency = account.getBalance().getCurrency();

            BigDecimal convertedAmountVal;
            if (accountCurrency.equals(currency)) {
                convertedAmountVal = request.amount();
            } else {
                convertedAmountVal = fxService.convert(request.amount(), currency, accountCurrency);
            }

            Money amount = Money.of(convertedAmountVal, accountCurrency);

            // Enforce daily limit
            if (card.getDailyLimit() != null) {
                LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
                BigDecimal dailySpent = transactionRepository.sumAmountByCardIdAndTypeAndStatusAndCreatedAtAfter(
                        card.getId(), startOfToday);
                BigDecimal newDailySpent = dailySpent.add(convertedAmountVal);
                if (newDailySpent.compareTo(card.getDailyLimit().getAmount()) > 0) {
                    transactionService.saveFailedTransaction(account.getId(), card.getId(), amount, "Rozliczenie płatności kartą (POS) - ODRZUCONA (Przekroczono limit dzienny)", request.merchantId(), request.authorizationCode(), request.amount(), currency);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Przekroczono dzienny limit transakcji na karcie.");
                }
            }

            // Enforce monthly limit
            if (card.getMonthlyLimit() != null) {
                LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                BigDecimal monthlySpent = transactionRepository.sumAmountByCardIdAndTypeAndStatusAndCreatedAtAfter(
                        card.getId(), startOfMonth);
                BigDecimal newMonthlySpent = monthlySpent.add(convertedAmountVal);
                if (newMonthlySpent.compareTo(card.getMonthlyLimit().getAmount()) > 0) {
                    transactionService.saveFailedTransaction(account.getId(), card.getId(), amount, "Rozliczenie płatności kartą (POS) - ODRZUCONA (Przekroczono limit miesięczny)", request.merchantId(), request.authorizationCode(), request.amount(), currency);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Przekroczono miesięczny limit transakcji na karcie.");
                }
            }

            BigDecimal available = account.getBalance().getAmount().subtract(account.getReservedBalance().getAmount());
            if (available.compareTo(convertedAmountVal) < 0) {
                transactionService.saveFailedTransaction(account.getId(), card.getId(), amount, "Rozliczenie płatności kartą (POS) - ODRZUCONA (Brak środków)", request.merchantId(), request.authorizationCode(), request.amount(), currency);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Brak wystarczających środków na rachunku.");
            }

            account.setBalance(account.getBalance().subtract(amount));
            accountRepository.save(account);

            String txDesc = "Rozliczenie płatności kartą (POS)";
            if (!accountCurrency.equals(currency)) {
                txDesc += " (" + request.amount() + " " + currency + ")";
            }

            transactionRepository.save(Transaction.builder()
                    .account(account)
                    .card(card)
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(cardDescription(txDesc, request.merchantId()))
                    .referenceId(authReference(request.authorizationCode()))
                    .build());

            return new IssuerStatusResponse("SETTLED");
        }
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
