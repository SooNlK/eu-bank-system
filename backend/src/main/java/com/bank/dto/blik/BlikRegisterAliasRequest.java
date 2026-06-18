package com.bank.dto.blik;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Żądanie rejestracji aliasu telefonu w systemie KLIK")
public record BlikRegisterAliasRequest(
        @Schema(description = "UUID rachunku bankowego") @NotNull UUID accountId,
        @Schema(description = "Numer telefonu w formacie międzynarodowym", example = "+48123456789") @NotBlank String phone
) {}
