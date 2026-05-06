package com.bank.api;

import com.bank.dto.account.AccountResponse;
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
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Zarządzanie rachunkami bankowymi klientów")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    @GetMapping
    @Operation(summary = "Lista rachunków zalogowanego klienta",
            description = "Zwraca wszystkie rachunki (STANDARD i JUNIOR) przypisane do zalogowanego klienta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista rachunków",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        // TODO: implement AccountService.getAccountsForCurrentUser()
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Szczegóły rachunku",
            description = "Zwraca dane konkretnego rachunku. Klient może pobrać tylko własne rachunki.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane rachunku",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rachunek nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do tego rachunku", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "UUID rachunku") @PathVariable UUID accountId) {
        // TODO: implement AccountService.getAccount()
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Saldo rachunku",
            description = "Zwraca aktualne saldo dostępne i saldo zarezerwowane.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo rachunku",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rachunek nie istnieje", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<AccountResponse> getBalance(
            @Parameter(description = "UUID rachunku") @PathVariable UUID accountId) {
        // TODO: implement AccountService.getBalance()
        return ResponseEntity.notFound().build();
    }
}
