package com.bank.dto.transfer;

import com.bank.domain.transfer.TransferChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Żądanie wykonania przelewu")
public record TransferRequest(
        @Schema(description = "UUID rachunku źródłowego") @NotNull UUID fromAccountId,
        @Schema(description = "Numer IBAN rachunku docelowego", example = "DE89370400440532013000") @NotBlank String toIban,
        @Schema(description = "Kwota przelewu", example = "250.00") @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Schema(description = "Waluta", example = "EUR") @NotBlank String currency,
        @Schema(description = "Kanał przelewu") @NotNull TransferChannel channel,
        @Schema(description = "Tytuł przelewu", example = "Opłata za usługę") String description
) {}
