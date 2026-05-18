package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.shared.IBAN;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import com.bank.domain.transfer.Transfer;
import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.transfer.TransferStatus;
import com.bank.dto.transfer.TransferRequest;
import com.bank.dto.transfer.TransferResponse;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.TransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private static final BigDecimal APPROVAL_LIMIT = new BigDecimal("15000.00");

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountService accountService
    ) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional
    public TransferResponse execute(TransferRequest request, String email) {
        Account fromAccount = accountService.getAccountEntity(request.fromAccountId(), email);
        String targetIban = normalizeIban(request.toIban());
        LocalDate valueDate = validateValueDate(request.valueDate(), request.channel());
        String currency = validateCurrency(request.currency());

        Account toAccount = accountRepository.findByAccountNumber_Value(targetIban)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek odbiorcy nie istnieje w tym banku"));

        Transfer transfer = Transfer.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(Money.of(request.amount(), currency))
                .channel(request.channel())
                .status(TransferStatus.PENDING)
                .description(request.description())
                .valueDate(valueDate)
                .requiresApproval(request.amount().compareTo(APPROVAL_LIMIT) > 0)
                .build();

        validateInternalTransfer(transfer);
        transfer = transferRepository.save(transfer);

        if (transfer.isRequiresApproval()) {
            transfer.setStatus(TransferStatus.PENDING_APPROVAL);
            return mapToResponse(transfer);
        }

        processInternalTransfer(transfer);
        return mapToResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getHistory(String email) {
        return transferRepository.findHistoryForCustomer(email).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse getById(UUID transferId, String email) {
        return transferRepository.findVisibleToCustomer(transferId, email)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono przelewu"));
    }

    @Transactional
    public TransferResponse approve(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono przelewu"));

        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Przelew nie oczekuje na zatwierdzenie");
        }

        transfer.setApprovedAt(LocalDateTime.now());
        processInternalTransfer(transfer);
        return mapToResponse(transfer);
    }

    @Transactional
    public TransferResponse reject(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono przelewu"));

        if (transfer.getStatus() == TransferStatus.COMPLETED || transfer.getStatus() == TransferStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Przelew został już przetworzony");
        }

        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectedAt(LocalDateTime.now());
        return mapToResponse(transfer);
    }

    private void processInternalTransfer(Transfer transfer) {
        transfer.setStatus(TransferStatus.PROCESSING);

        try {
            Account fromAccount = transfer.getFromAccount();
            Account toAccount = transfer.getToAccount();
            Money amount = transfer.getAmount();

            if (fromAccount.getBalance().getAmount().compareTo(amount.getAmount()) < 0) {
                transfer.setStatus(TransferStatus.FAILED);
                return;
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            transactionRepository.save(Transaction.builder()
                    .account(fromAccount)
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(transfer.getDescription())
                    .referenceId(transfer.getId().toString())
                    .build());

            transactionRepository.save(Transaction.builder()
                    .account(toAccount)
                    .amount(amount)
                    .type(TransactionType.CREDIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(transfer.getDescription())
                    .referenceId(transfer.getId().toString())
                    .build());

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(LocalDateTime.now());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            transfer.setStatus(TransferStatus.FAILED);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Przelew nie powiódł się");
        }
    }

    private void validateInternalTransfer(Transfer transfer) {
        if (transfer.getChannel() != TransferChannel.INTERNAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obsługiwane są wyłącznie przelewy wewnętrzne");
        }
        if (transfer.getFromAccount().getId().equals(transfer.getToAccount().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek źródłowy i docelowy muszą być różne");
        }
        if (transfer.getFromAccount().getStatus() != AccountStatus.ACTIVE
                || transfer.getToAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Oba rachunki muszą być aktywne");
        }
        if (!transfer.getFromAccount().getBalance().getCurrency().equals(transfer.getAmount().getCurrency())
                || !transfer.getToAccount().getBalance().getCurrency().equals(transfer.getAmount().getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waluta przelewu musi być zgodna z walutą obu rachunków");
        }
    }

    private String normalizeIban(String value) {
        try {
            return IBAN.of(value).getValue();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private LocalDate validateValueDate(LocalDate requestedValueDate, TransferChannel channel) {
        LocalDate today = LocalDate.now();
        LocalDate valueDate = requestedValueDate == null ? today : requestedValueDate;

        if (valueDate.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data waluty nie może być z przeszłości");
        }
        if (channel == TransferChannel.INTERNAL && !valueDate.equals(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data waluty dla przelewu wewnętrznego musi być dzisiejsza");
        }
        return valueDate;
    }

    private String validateCurrency(String currency) {
        String normalized = currency == null ? "" : currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waluta musi być 3-literowym kodem ISO");
        }
        return normalized;
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount() == null ? null : transfer.getToAccount().getId(),
                transfer.getAmount().getAmount(),
                transfer.getAmount().getCurrency(),
                transfer.getChannel(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getValueDate(),
                transfer.isRequiresApproval(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}
