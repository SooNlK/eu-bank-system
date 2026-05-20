package com.bank.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dane rejestracji nowego konta Junior")
public record RegisterJuniorRequest(
        @Schema(description = "Adres e-mail dziecka", example = "tomek.kowalski@example.com")
        @NotBlank @Email
        String email,

        @Schema(description = "Hasło dziecka (min. 8 znaków)", example = "Secure@123")
        @NotBlank @Size(min = 8)
        String password,

        @Schema(description = "Imię dziecka", example = "Tomasz")
        @NotBlank @Size(max = 100)
        String firstName,

        @Schema(description = "Nazwisko dziecka", example = "Kowalski")
        @NotBlank @Size(max = 100)
        String lastName,

        @Schema(description = "Numer paszportu lub legitymacji dziecka", example = "C01X00T47")
        @NotBlank
        @jakarta.validation.constraints.Pattern(
            regexp = "^[CFGHJKLMNPRTVWXYZcfghjklmnprtvwxyz0-9]{9}$", 
            message = "Numer paszportu musi mieć 9 znaków i zawierać tylko cyfry oraz dozwolone litery (C,F,G,H,J,K,L,M,N,P,R,T,V,W,X,Y,Z)"
        )
        String passportNumber,

        @Schema(description = "Data urodzenia dziecka (wiek 7-13 lat)", example = "2016-05-19")
        @NotNull
        LocalDate dateOfBirth,

        @Schema(description = "UUID konta rodzica, z którym powiązane jest konto Junior")
        @NotNull
        UUID parentAccountId
) {}
