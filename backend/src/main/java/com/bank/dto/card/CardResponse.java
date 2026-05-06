package com.bank.dto.card;

import com.bank.domain.card.CardStatus;
import com.bank.domain.card.CardType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dane karty płatniczej")
public record CardResponse(
        @Schema(description = "UUID karty") UUID id,
        @Schema(description = "UUID powiązanego rachunku") UUID accountId,
        @Schema(description = "Ostatnie 4 cyfry numeru karty", example = "4242") String last4,
        @Schema(description = "Typ karty") CardType type,
        @Schema(description = "Status karty") CardStatus status,
        @Schema(description = "Data ważności", example = "2028-12-31") LocalDate expiresAt,
        @Schema(description = "Dzienny limit transakcyjny") BigDecimal dailyLimit,
        @Schema(description = "Miesięczny limit transakcyjny") BigDecimal monthlyLimit,
        @Schema(description = "Data wydania karty") LocalDateTime createdAt
) {}
