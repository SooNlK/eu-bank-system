package com.bank.api;

import com.bank.dto.blik.BlikGenerateRequest;
import com.bank.dto.blik.BlikGenerateResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blik")
@Tag(name = "BLIK", description = "Generowanie i weryfikacja kodów BLIK (ważne 120 sekund)")
@SecurityRequirement(name = "bearerAuth")
public class BlikController {

    @PostMapping("/generate")
    @Operation(summary = "Generowanie kodu BLIK",
            description = "Generuje jednorazowy 6-cyfrowy kod BLIK dla wskazanego rachunku. " +
                    "Kod jest ważny przez 120 sekund i może być użyty tylko raz.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kod BLIK wygenerowany",
                    content = @Content(schema = @Schema(implementation = BlikGenerateResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rachunek nie istnieje", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do rachunku", content = @Content),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<BlikGenerateResponse> generateCode(@Valid @RequestBody BlikGenerateRequest request) {
        // TODO: implement BlikService.generate()
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify")
    @Operation(summary = "Weryfikacja kodu BLIK",
            description = "Weryfikuje podany kod BLIK i oznacza go jako użyty. " +
                    "Używane przez system płatności przy autoryzacji transakcji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kod poprawny i zaakceptowany", content = @Content),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowy, wygasły lub już użyty kod BLIK", content = @Content),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji", content = @Content)
    })
    public ResponseEntity<Void> verifyCode(
            @io.swagger.v3.oas.annotations.Parameter(description = "Kod BLIK do weryfikacji", example = "123456")
            @RequestParam String code) {
        // TODO: implement BlikService.verify()
        return ResponseEntity.ok().build();
    }
}
