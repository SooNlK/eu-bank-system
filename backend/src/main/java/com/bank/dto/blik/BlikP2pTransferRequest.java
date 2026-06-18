package com.bank.dto.blik;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Żądanie przelewu na telefon P2P KLIK")
public record BlikP2pTransferRequest(
        @Schema(description = "UUID rachunku nadawcy") @NotNull UUID fromAccountId,
        @Schema(description = "Numer telefonu odbiorcy", example = "+48987654321") @NotBlank String toPhone,
        @Schema(description = "Kwota przelewu", example = "50.00") @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Schema(description = "Waluta transakcji (EUR)", example = "EUR") @NotBlank String currency,
        @Schema(description = "Tytuł przelewu", example = "Przelew na telefon") String description
) {}
