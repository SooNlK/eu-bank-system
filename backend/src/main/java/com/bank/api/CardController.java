package com.bank.api;

import com.bank.dto.card.CardResponse;
import com.bank.dto.card.IssueCardRequest;
import com.bank.dto.card.IssueCardResponse;
import com.bank.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Zarządzanie kartami płatniczymi (debetowe i prepaid)")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @Operation(summary = "Wydanie karty przez zewnętrzną sieć kartową",
            description = "Zamawia kartę w module kart płatniczych, zapisuje token i metadane karty w banku. Pełny PAN i CVV są zwracane tylko raz.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Karta zamówiona",
                    content = @Content(schema = @Schema(implementation = IssueCardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane", content = @Content),
            @ApiResponse(responseCode = "502", description = "Błąd komunikacji z modułem kart", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<IssueCardResponse> issueCard(
            @Valid @RequestBody IssueCardRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.issue(request, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Lista kart zalogowanego klienta",
            description = "Zwraca wszystkie karty debetowe i prepaid przypisane do rachunków klienta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista kart",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<CardResponse>> getMyCards(Authentication authentication) {
        return ResponseEntity.ok(cardService.getCardsForCurrentUser(authentication.getName()));
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
            @Parameter(description = "UUID karty") @PathVariable UUID cardId,
            Authentication authentication) {
        return ResponseEntity.ok(cardService.getById(cardId, authentication.getName()));
    }

    @GetMapping("/{cardId}/sensitive")
    @Operation(summary = "Pobierz pełne dane karty (PAN, CVV, data ważności)")
    public ResponseEntity<Map<String, Object>> getSensitiveCard(
            @Parameter(description = "UUID karty") @PathVariable UUID cardId,
            Authentication authentication) {
        return ResponseEntity.ok(cardService.getSensitiveDetails(cardId, authentication.getName()));
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
            @Parameter(description = "UUID karty") @PathVariable UUID cardId,
            Authentication authentication) {
        return ResponseEntity.ok(cardService.block(cardId, authentication.getName()));
    }

    @PostMapping("/{cardId}/unblock")
    @Operation(summary = "Odblokowanie karty")
    public ResponseEntity<CardResponse> unblockCard(
            @Parameter(description = "UUID karty") @PathVariable UUID cardId,
            Authentication authentication) {
        return ResponseEntity.ok(cardService.unblock(cardId, authentication.getName()));
    }

    @PostMapping("/{cardId}/activate")
    @Operation(summary = "Aktywacja karty fizycznej lub prepaid")
    public ResponseEntity<CardResponse> activateCard(
            @Parameter(description = "UUID karty") @PathVariable UUID cardId,
            Authentication authentication) {
        return ResponseEntity.ok(cardService.activate(cardId, authentication.getName()));
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
            @RequestBody Map<String, BigDecimal> limits,
            Authentication authentication) {
        return ResponseEntity.ok(cardService.updateLimits(
                cardId,
                limits.get("dailyLimit"),
                limits.get("monthlyLimit"),
                authentication.getName()
        ));
    }
}
