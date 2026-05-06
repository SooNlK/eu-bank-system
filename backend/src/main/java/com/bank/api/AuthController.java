package com.bank.api;

import com.bank.dto.auth.LoginRequest;
import com.bank.dto.auth.LoginResponse;
import com.bank.dto.auth.RegisterRequest;
import com.bank.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Rejestracja i logowanie klientów")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    @Operation(summary = "Logowanie klienta", description = "Zwraca token JWT po poprawnym uwierzytelnieniu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zalogowano pomyślnie",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nieprawidłowy e-mail lub hasło", content = @Content),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji danych wejściowych", content = @Content)
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        String token = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowego klienta",
            description = "Tworzy konto klienta. Domyślnie konto jest w statusie ACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Klient zarejestrowany pomyślnie", content = @Content),
            @ApiResponse(responseCode = "409", description = "Klient z podanym e-mailem już istnieje", content = @Content),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji danych wejściowych", content = @Content)
    })
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        // TODO: implement CustomerService.register()
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
