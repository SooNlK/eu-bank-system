package com.bank.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Wynik wydania karty. Pełne dane karty są zwracane tylko raz.")
public record IssueCardResponse(
        CardResponse card,
        @Schema(description = "Pełny numer PAN zwracany jednorazowo") String fullPan,
        @Schema(description = "CVV zwracany jednorazowo") String cvv,
        String message
) {}
