package com.bank.api;

import com.bank.dto.blik.BlikConfirmRequest;
import com.bank.dto.blik.BlikGenerateRequest;
import com.bank.dto.blik.BlikGenerateResponse;
import com.bank.dto.blik.BlikPendingResponse;
import com.bank.dto.blik.BlikRegisterAliasRequest;
import com.bank.dto.blik.BlikP2pTransferRequest;
import com.bank.dto.transfer.TransferResponse;
import com.bank.client.klik.KlikClient;
import com.bank.service.KlikService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blik")
@Tag(name = "BLIK", description = "Generowanie i zatwierdzanie transakcji mobilnych KLIK (odpowiednik BLIK)")
@SecurityRequirement(name = "bearerAuth")
public class BlikController {

    private final KlikService klikService;

    public BlikController(KlikService klikService) {
        this.klikService = klikService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generowanie kodu KLIK",
            description = "Generuje jednorazowy 6-cyfrowy kod KLIK dla wskazanego rachunku. " +
                    "Kod jest ważny przez 120 sekund i może być użyty tylko raz.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kod KLIK wygenerowany",
                    content = @Content(schema = @Schema(implementation = BlikGenerateResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rachunek nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do rachunku", content = @Content),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<BlikGenerateResponse> generateCode(
            @Valid @RequestBody BlikGenerateRequest request,
            Authentication authentication
    ) {
        BlikGenerateResponse response = klikService.generateCode(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pending")
    @Operation(summary = "Pobranie oczekujących transakcji KLIK",
            description = "Zwraca listę transakcji KLIK zainicjowanych kodem wygenerowanym przez zalogowanego klienta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista oczekujących transakcji",
                    content = @Content(schema = @Schema(implementation = BlikPendingResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<BlikPendingResponse>> getPendingTransactions(Authentication authentication) {
        List<BlikPendingResponse> pending = klikService.getPendingTransactions(authentication.getName()).stream()
                .map(tx -> new BlikPendingResponse(
                        tx.getId(),
                        tx.getAccount().getId(),
                        tx.getAccount().getAccountNumber().getValue(),
                        tx.getAmount(),
                        tx.getCurrency(),
                        tx.getMerchantName(),
                        tx.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Zatwierdzenie transakcji KLIK",
            description = "Zatwierdza (ACCEPTED) lub odrzuca (REJECTED) oczekującą transakcję KLIK o podanym UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decyzja została przekazana do KLIK"),
            @ApiResponse(responseCode = "404", description = "Transakcja nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do autoryzacji tej transakcji", content = @Content),
            @ApiResponse(responseCode = "400", description = "Transakcja już zamknięta lub błąd walidacji", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<Void> confirmTransaction(
            @Valid @RequestBody BlikConfirmRequest request,
            Authentication authentication
    ) {
        klikService.confirmTransaction(request.transactionId(), request.status(), authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/p2p/register")
    @Operation(summary = "Rejestracja aliasu P2P (telefon)",
            description = "Rejestruje numer telefonu powiązany z IBAN wybranego rachunku w systemie KLIK.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alias zarejestrowany pomyślnie",
                    content = @Content(schema = @Schema(implementation = KlikClient.AliasRegisterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rachunek nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do rachunku", content = @Content),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<KlikClient.AliasRegisterResponse> registerAlias(
            @Valid @RequestBody BlikRegisterAliasRequest request,
            Authentication authentication
    ) {
        KlikClient.AliasRegisterResponse response = klikService.registerAlias(
                request.accountId(), request.phone(), authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/p2p/unregister")
    @Operation(summary = "Wyrejestrowanie aliasu P2P (telefon)",
            description = "Usuwa rejestrację numeru telefonu w systemie KLIK.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Alias usunięty pomyślnie"),
            @ApiResponse(responseCode = "404", description = "Alias nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu (alias nie należy do zalogowanego klienta)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<Void> unregisterAlias(
            @RequestParam String phone,
            Authentication authentication
    ) {
        klikService.deleteAlias(phone, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/p2p/transfer")
    @Operation(summary = "Przelew na telefon P2P KLIK",
            description = "Wykonuje wyszukanie IBAN powiązanego z numerem telefonu w KLIK i zleca przelew SEPA Instant lub wewnętrzny.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Przelew zlecony pomyślnie",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "404", description = "Odbiorca nie posiada zarejestrowanego numeru w KLIK", content = @Content),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji lub niewystarczające środki", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<TransferResponse> p2pTransfer(
            @Valid @RequestBody BlikP2pTransferRequest request,
            Authentication authentication
    ) {
        TransferResponse response = klikService.p2pTransfer(request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
