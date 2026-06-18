package com.bank.dto.blik;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Zatwierdzenie lub odrzucenie transakcji KLIK")
public record BlikConfirmRequest(
        @Schema(description = "UUID transakcji KLIK do zatwierdzenia") @NotNull UUID transactionId,
        @Schema(description = "Decyzja klienta", allowableValues = {"ACCEPTED", "REJECTED"})
        @NotNull
        @Pattern(regexp = "^(ACCEPTED|REJECTED)$", message = "Decyzja musi wynosić ACCEPTED lub REJECTED")
        String status
) {}
