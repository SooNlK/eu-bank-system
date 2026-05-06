package com.bank.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Odpowiedź po pomyślnym logowaniu")
public record LoginResponse(
        @Schema(description = "Token JWT do autoryzacji kolejnych żądań")
        String token,

        @Schema(description = "Typ tokenu", example = "Bearer")
        String tokenType
) {
    public LoginResponse(String token) {
        this(token, "Bearer");
    }
}
