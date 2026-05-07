package com.bank.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dane rejestracji nowego klienta")
public record RegisterRequest(
        @Schema(description = "Adres e-mail", example = "jan.kowalski@example.com")
        @NotBlank @Email
        String email,

        @Schema(description = "Hasło (min. 8 znaków)", example = "Secure@123")
        @NotBlank @Size(min = 8)
        String password,

        @Schema(description = "Imię", example = "Jan")
        @NotBlank @Size(max = 100)
        String firstName,

        @Schema(description = "Nazwisko", example = "Kowalski")
        @NotBlank @Size(max = 100)
        String lastName
) {}
