package com.bank.dto.blik;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dane oczekującej transakcji KLIK")
public record BlikPendingResponse(
        @Schema(description = "Identyfikator transakcji z systemu KLIK") UUID transactionId,
        @Schema(description = "Identyfikator konta bankowego") UUID accountId,
        @Schema(description = "Numer konta bankowego (IBAN)") String accountNumber,
        @Schema(description = "Kwota transakcji") BigDecimal amount,
        @Schema(description = "Waluta transakcji") String currency,
        @Schema(description = "Nazwa punktu sprzedaży (merchanta)") String merchantName,
        @Schema(description = "Data utworzenia żądania") LocalDateTime createdAt
) {}
