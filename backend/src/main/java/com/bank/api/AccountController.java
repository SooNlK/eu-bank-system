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

import com.bank.service.AccountService;
import com.bank.service.TransactionService;
import com.bank.dto.transaction.TransactionResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Zarządzanie rachunkami bankowymi klientów")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Lista rachunków zalogowanego klienta",
            description = "Zwraca wszystkie rachunki (STANDARD i JUNIOR) przypisane do zalogowanego klienta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista rachunków",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccountsForCurrentUser(authentication.getName()));
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
            @Parameter(description = "UUID rachunku") @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccount(accountId, authentication.getName()));
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
            @Parameter(description = "UUID rachunku") @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.getBalance(accountId, authentication.getName()));
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Historia transakcji",
            description = "Zwraca historię transakcji dla wybranego rachunku.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista transakcji",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rachunek nie istnieje", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @Parameter(description = "UUID rachunku") @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(transactionService.getTransactionsForAccount(accountId, authentication.getName()));
    }

    @GetMapping("/{accountId}/transactions/{transactionId}")
    @Operation(summary = "Szczegóły transakcji",
            description = "Zwraca szczegóły pojedynczej transakcji dla wybranego rachunku.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Szczegóły transakcji",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transakcja nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "UUID rachunku") @PathVariable UUID accountId,
            @Parameter(description = "UUID transakcji") @PathVariable UUID transactionId,
            Authentication authentication) {
        return ResponseEntity.ok(transactionService.getTransaction(accountId, transactionId, authentication.getName()));
    }
}
