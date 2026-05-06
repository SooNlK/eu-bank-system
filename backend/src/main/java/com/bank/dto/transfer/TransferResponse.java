package com.bank.dto.transfer;

import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.transfer.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dane przelewu")
public record TransferResponse(
        @Schema(description = "UUID przelewu") UUID id,
        @Schema(description = "UUID rachunku źródłowego") UUID fromAccountId,
        @Schema(description = "UUID rachunku docelowego (jeśli wewnętrzny)") UUID toAccountId,
        @Schema(description = "Kwota") BigDecimal amount,
        @Schema(description = "Waluta") String currency,
        @Schema(description = "Kanał przelewu") TransferChannel channel,
        @Schema(description = "Status przelewu") TransferStatus status,
        @Schema(description = "Tytuł przelewu") String description,
        @Schema(description = "Wymaga zatwierdzenia") boolean requiresApproval,
        @Schema(description = "Data zlecenia") LocalDateTime createdAt,
        @Schema(description = "Data realizacji") LocalDateTime completedAt
) {}
