package com.bank.dto.transaction;

import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dane transakcji")
public record TransactionResponse(
        @Schema(description = "UUID transakcji") UUID id,
        @Schema(description = "Kwota transakcji") BigDecimal amount,
        @Schema(description = "Waluta transakcji") String currency,
        @Schema(description = "Typ transakcji") TransactionType type,
        @Schema(description = "Status transakcji") TransactionStatus status,
        @Schema(description = "Opis transakcji") String description,
        @Schema(description = "ID referencyjne") String referenceId,
        @Schema(description = "Data transakcji") LocalDateTime createdAt
) {}
