package com.bank.dto.account;

import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dane rachunku bankowego")
public record AccountResponse(
        @Schema(description = "UUID rachunku") UUID id,
        @Schema(description = "Numer rachunku (IBAN)", example = "PL61109010140000071219812874") String accountNumber,
        @Schema(description = "Typ rachunku") AccountType type,
        @Schema(description = "Dostępne saldo") BigDecimal balance,
        @Schema(description = "Zablokowane środki") BigDecimal reservedBalance,
        @Schema(description = "Waluta", example = "EUR") String currency,
        @Schema(description = "Status rachunku") AccountStatus status,
        @Schema(description = "Data otwarcia rachunku") LocalDateTime createdAt
) {}
