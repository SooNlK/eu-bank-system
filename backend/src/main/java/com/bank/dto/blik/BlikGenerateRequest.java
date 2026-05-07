package com.bank.dto.blik;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Żądanie generowania kodu BLIK")
public record BlikGenerateRequest(
        @Schema(description = "UUID rachunku, z którego zostanie wygenerowany kod BLIK") @NotNull UUID accountId
) {}
