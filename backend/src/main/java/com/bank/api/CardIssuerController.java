package com.bank.api;

import com.bank.dto.cardnetwork.IssuerAuthorizationRequest;
import com.bank.dto.cardnetwork.IssuerAuthorizationResponse;
import com.bank.dto.cardnetwork.IssuerCaptureRequest;
import com.bank.dto.cardnetwork.IssuerRefundRequest;
import com.bank.dto.cardnetwork.IssuerStatusResponse;
import com.bank.service.CardIssuerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Card Issuer API", description = "Endpointy wywoływane przez zewnętrzną sieć kartową")
public class CardIssuerController {

    private final CardIssuerService cardIssuerService;

    public CardIssuerController(CardIssuerService cardIssuerService) {
        this.cardIssuerService = cardIssuerService;
    }

    @PostMapping({"/api/v1/authorize", "/authorize"})
    @Operation(summary = "Autoryzacja płatności kartą",
            description = "Sprawdza status rachunku, walutę i dostępne środki, a następnie rezerwuje kwotę transakcji.")
    public ResponseEntity<IssuerAuthorizationResponse> authorize(@Valid @RequestBody IssuerAuthorizationRequest request) {
        return ResponseEntity.ok(cardIssuerService.authorize(request));
    }

    @PostMapping({"/api/v1/capture", "/capture"})
    @Operation(summary = "Rozliczenie autoryzowanej płatności kartą",
            description = "Zdejmuje rezerwację i księguje finalny debet na rachunku klienta.")
    public ResponseEntity<IssuerStatusResponse> capture(@Valid @RequestBody IssuerCaptureRequest request) {
        return ResponseEntity.ok(cardIssuerService.capture(request));
    }

    @PostMapping({"/api/v1/refund", "/refund"})
    @Operation(summary = "Zwrot płatności kartą",
            description = "Księguje uznanie rachunku klienta po zwrocie z sieci kartowej.")
    public ResponseEntity<IssuerStatusResponse> refund(@Valid @RequestBody IssuerRefundRequest request) {
        return ResponseEntity.ok(cardIssuerService.refund(request));
    }
}
