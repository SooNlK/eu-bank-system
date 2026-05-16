package com.bank.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Wynik rejestracji klienta")
public record RegisterResponse(
        @Schema(description = "Adres e-mail klienta", example = "jan.kowalski@example.com")
        String email,

        @Schema(description = "Wygenerowany niemiecki IBAN rachunku", example = "DE02100110010012345678")
        String accountNumber
) {}
