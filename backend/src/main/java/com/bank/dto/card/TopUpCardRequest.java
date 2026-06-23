package com.bank.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Żądanie doładowania karty prepaid")
public record TopUpCardRequest(
        @NotNull(message = "Rachunek źródłowy jest wymagany")
        @Schema(description = "UUID rachunku, z którego pobrane zostaną środki")
        UUID sourceAccountId,

        @NotNull(message = "Kwota jest wymagana")
        @DecimalMin(value = "0.01", message = "Kwota doładowania musi być większa niż 0")
        @Schema(description = "Kwota doładowania")
        BigDecimal amount
) {}
