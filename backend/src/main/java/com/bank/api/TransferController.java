package com.bank.api;

import com.bank.dto.transfer.TransferRequest;
import com.bank.dto.transfer.TransferResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Przelewy bankowe: wewnętrzne, SEPA, SEPA Instant, TARGET2, SWIFT")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    @PostMapping
    @Operation(summary = "Zlecenie przelewu",
            description = "Inicjuje przelew z rachunku zalogowanego klienta. " +
                    "Przelewy powyżej 15 000 EUR wymagają zatwierdzenia (requiresApproval=true).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Przelew zlecony",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji lub niewystarczające środki", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do rachunku źródłowego", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody TransferRequest request) {
        // TODO: implement TransferService.execute()
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @Operation(summary = "Historia przelewów",
            description = "Zwraca historię wszystkich przelewów wychodzących i przychodzących dla zalogowanego klienta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista przelewów",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<TransferResponse>> getTransfers() {
        // TODO: implement TransferService.getHistory()
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Szczegóły przelewu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane przelewu",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "404", description = "Przelew nie istnieje", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<TransferResponse> getTransfer(
            @Parameter(description = "UUID przelewu") @PathVariable UUID transferId) {
        // TODO: implement TransferService.getById()
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{transferId}/approve")
    @Operation(summary = "Zatwierdzenie przelewu wymagającego autoryzacji",
            description = "Zatwierdza przelew o statusie PENDING_APPROVAL. Dostępne tylko dla uprawnionych użytkowników.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Przelew zatwierdzony",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "404", description = "Przelew nie istnieje", content = @Content),
            @ApiResponse(responseCode = "409", description = "Przelew nie wymaga zatwierdzenia lub jest już przetworzony", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<TransferResponse> approveTransfer(
            @Parameter(description = "UUID przelewu") @PathVariable UUID transferId) {
        // TODO: implement TransferService.approve()
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{transferId}/reject")
    @Operation(summary = "Odrzucenie przelewu wymagającego autoryzacji")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Przelew odrzucony",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "404", description = "Przelew nie istnieje", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<TransferResponse> rejectTransfer(
            @Parameter(description = "UUID przelewu") @PathVariable UUID transferId) {
        // TODO: implement TransferService.reject()
        return ResponseEntity.notFound().build();
    }
}
