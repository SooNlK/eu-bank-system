package com.bank.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dane do logowania")
public record LoginRequest(
        @Schema(description = "Adres e-mail klienta", example = "jan.kowalski@example.com")
        @NotBlank @Email
        String email,

        @Schema(description = "Hasło klienta", example = "Secure@123")
        @NotBlank @Size(min = 8)
        String password
) {}
