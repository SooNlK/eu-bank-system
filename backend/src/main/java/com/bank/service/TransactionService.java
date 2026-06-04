package com.bank.service;

import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.domain.transfer.Transfer;
import com.bank.repository.TransactionRepository;
import com.bank.repository.TransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bank.dto.transaction.TransactionResponse;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransferRepository transferRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountService accountService,
            TransferRepository transferRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.transferRepository = transferRepository;
    }

    public List<TransactionResponse> getTransactionsForAccount(UUID accountId, String email) {
        // Will throw if account doesn't belong to customer
        accountService.getAccountEntity(accountId, email);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransaction(UUID accountId, UUID transactionId, String email) {
        accountService.getAccountEntity(accountId, email);
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transakcja nie istnieje"));
        if (!tx.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do tej transakcji");
        }
        return mapToResponse(tx);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        String counterpartyName = null;
        String counterpartyIban = null;

        if (transaction.getReferenceId() != null) {
            try {
                UUID transferId = UUID.fromString(transaction.getReferenceId());
                Optional<Transfer> transferOpt = transferRepository.findById(transferId);
                if (transferOpt.isPresent()) {
                    Transfer transfer = transferOpt.get();
                    if (transaction.getType() == TransactionType.DEBIT) {
                        if (transfer.getToAccount() != null) {
                            if (transfer.getToAccount().getCustomer() != null) {
                                counterpartyName = transfer.getToAccount().getCustomer().getFirstName() + " " +
                                        transfer.getToAccount().getCustomer().getLastName();
                            }
                            if (transfer.getToAccount().getAccountNumber() != null) {
                                counterpartyIban = transfer.getToAccount().getAccountNumber().getValue();
                            }
                        } else {
                            counterpartyName = transfer.getBeneficiaryName();
                            counterpartyIban = transfer.getToIban();
                        }
                    } else if (transaction.getType() == TransactionType.CREDIT) {
                        if (transfer.getFromAccount() != null) {
                            if (transfer.getFromAccount().getCustomer() != null) {
                                counterpartyName = transfer.getFromAccount().getCustomer().getFirstName() + " " +
                                        transfer.getFromAccount().getCustomer().getLastName();
                            }
                            if (transfer.getFromAccount().getAccountNumber() != null) {
                                counterpartyIban = transfer.getFromAccount().getAccountNumber().getValue();
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Ignore, referenceId is not a UUID
            }
        }

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount().getAmount(),
                transaction.getAmount().getCurrency(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getReferenceId(),
                transaction.getCreatedAt(),
                counterpartyName,
                counterpartyIban
        );
    }
}
