package com.bank.dto.card;

import com.bank.domain.card.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Żądanie wydania karty przez zewnętrzną sieć kartową")
public record IssueCardRequest(
        @NotNull
        @Schema(description = "UUID rachunku, do którego ma być przypisana karta") UUID accountId,

        @NotNull
        @Schema(description = "Typ karty zgodny z modułem kart") CardType cardType,

        @DecimalMin(value = "0.00")
        @Schema(description = "Saldo początkowe dla karty prepaid") BigDecimal initialBalance,

        @DecimalMin(value = "0.00")
        @Schema(description = "Dzienny limit transakcyjny") BigDecimal dailyLimit,

        @DecimalMin(value = "0.00")
        @Schema(description = "Miesięczny limit transakcyjny") BigDecimal monthlyLimit
) {}
