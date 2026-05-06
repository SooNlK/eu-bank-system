package com.bank.api;

import com.bank.dto.card.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Zarządzanie kartami płatniczymi (debetowe i prepaid)")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    @GetMapping
    @Operation(summary = "Lista kart zalogowanego klienta",
            description = "Zwraca wszystkie karty debetowe i prepaid przypisane do rachunków klienta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista kart",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<CardResponse>> getMyCards() {
        // TODO: implement CardService.getCardsForCurrentUser()
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Szczegóły karty")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane karty",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Karta nie istnieje", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<CardResponse> getCard(
            @Parameter(description = "UUID karty") @PathVariable UUID cardId) {
        // TODO: implement CardService.getById()
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{cardId}/block")
    @Operation(summary = "Zablokowanie karty",
            description = "Natychmiast blokuje kartę. Operacja jest nieodwracalna bez kontaktu z bankiem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Karta zablokowana",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Karta nie istnieje", content = @Content),
            @ApiResponse(responseCode = "409", description = "Karta jest już zablokowana", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "UUID karty") @PathVariable UUID cardId) {
        // TODO: implement CardService.block()
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{cardId}/limits")
    @Operation(summary = "Aktualizacja limitów karty",
            description = "Ustawia dzienny i/lub miesięczny limit transakcyjny karty.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Limity zaktualizowane",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Karta nie istnieje", content = @Content),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe wartości limitów", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<CardResponse> updateLimits(
            @Parameter(description = "UUID karty") @PathVariable UUID cardId,
            @RequestBody java.util.Map<String, java.math.BigDecimal> limits) {
        // TODO: implement CardService.updateLimits()
        return ResponseEntity.notFound().build();
    }
}
