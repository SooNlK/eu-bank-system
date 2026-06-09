package com.bank.dto.transfer;

import com.bank.domain.transfer.TransferChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Żądanie wykonania przelewu")
public record TransferRequest(
        @Schema(description = "UUID rachunku źródłowego") @NotNull UUID fromAccountId,
        @Schema(description = "Numer IBAN lub rachunku docelowego", example = "DE89370400440532013000") @NotBlank String toIban,
        @Schema(description = "Kwota przelewu", example = "250.00") @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Schema(description = "Waluta rachunku klienta", example = "EUR") @NotBlank String currency,
        @Schema(description = "Data waluty", example = "2026-05-18") LocalDate valueDate,
        @Schema(description = "Kanał przelewu") @NotNull TransferChannel channel,
        @Schema(description = "Tytuł przelewu", example = "Opłata za usługę") String description,
        @Schema(description = "BIC / SWIFT banku odbiorcy (wymagany dla TARGET i SWIFT)", example = "BANKDEXX") String toBic,
        @Schema(description = "Nazwa odbiorcy (dla przelewów zewnętrznych)", example = "Jan Kowalski") String beneficiaryName,
        @Schema(description = "Podział opłat SWIFT: SHAR=podzielone, DEBT=nadawca, CRED=odbiorca (tylko SWIFT)", example = "SHAR") String chargeBearer,
        @Schema(description = "Waluta docelowa SWIFT (tylko SWIFT, np. USD, GBP)", example = "EUR") String swiftTargetCurrency
) {}

