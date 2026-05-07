package com.bank.dto.blik;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Odpowiedź z wygenerowanym kodem BLIK")
public record BlikGenerateResponse(
        @Schema(description = "UUID rekordu BLIK") UUID id,
        @Schema(description = "Sześciocyfrowy kod BLIK", example = "123456") String code,
        @Schema(description = "Data ważności kodu") LocalDateTime expiresAt
) {}
