package com.bank.dto.transfer;

import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.transfer.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dane przelewu")
public record TransferResponse(
        @Schema(description = "UUID przelewu") UUID id,
        @Schema(description = "UUID rachunku źródłowego") UUID fromAccountId,
        @Schema(description = "UUID rachunku docelowego (jeśli wewnętrzny)") UUID toAccountId,
        @Schema(description = "IBAN odbiorcy zewnętrznego (jeśli SEPA/TARGET/SWIFT)") String toIban,
        @Schema(description = "BIC banku odbiorcy") String toBic,
        @Schema(description = "Nazwa odbiorcy") String beneficiaryName,
        @Schema(description = "Kwota (w walucie rachunku klienta)") BigDecimal amount,
        @Schema(description = "Waluta rachunku klienta") String currency,
        @Schema(description = "Kanał przelewu") TransferChannel channel,
        @Schema(description = "Status przelewu") TransferStatus status,
        @Schema(description = "Tytuł przelewu") String description,
        @Schema(description = "Data waluty") LocalDate valueDate,
        @Schema(description = "Wymaga zatwierdzenia") boolean requiresApproval,
        @Schema(description = "Data zlecenia") LocalDateTime createdAt,
        @Schema(description = "Data realizacji") LocalDateTime completedAt,
        // Pola SWIFT (null dla innych kanałów)
        @Schema(description = "ID wiadomości SWIFT (pacs.008)") String swiftMsgId,
        @Schema(description = "UETR – end-to-end tracking") String swiftUetr,
        @Schema(description = "Trasa SWIFT") String swiftRoute,
        @Schema(description = "Opłata SWIFT (EUR)") BigDecimal swiftFee,
        @Schema(description = "Kurs wymiany EUR → waluta docelowa") BigDecimal swiftFxRate,
        @Schema(description = "Waluta docelowa SWIFT") String swiftTargetCurrency,
        @Schema(description = "Podział opłat (SHA/OUR/BEN)") String swiftChargeBearer
) {}

