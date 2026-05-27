package com.bank.service;

import com.bank.client.cardnetwork.CardNetworkClient;
import com.bank.client.cardnetwork.CardNetworkCardResponse;
import com.bank.client.cardnetwork.CardNetworkIssueResponse;
import com.bank.client.cardnetwork.CardNetworkStatusResponse;
import com.bank.domain.account.Account;
import com.bank.domain.card.Card;
import com.bank.domain.card.CardStatus;
import com.bank.domain.card.CardType;
import com.bank.domain.customer.Customer;
import com.bank.domain.shared.Money;
import com.bank.dto.card.CardResponse;
import com.bank.dto.card.IssueCardRequest;
import com.bank.dto.card.IssueCardResponse;
import com.bank.repository.CardRepository;
import com.bank.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CardService {

    private static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal DEFAULT_MONTHLY_LIMIT = new BigDecimal("5000.00");

    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final AccountService accountService;
    private final CardNetworkClient cardNetworkClient;

    public CardService(CardRepository cardRepository,
                       CustomerRepository customerRepository,
                       AccountService accountService,
                       CardNetworkClient cardNetworkClient) {
        this.cardRepository = cardRepository;
        this.customerRepository = customerRepository;
        this.accountService = accountService;
        this.cardNetworkClient = cardNetworkClient;
    }

    @Transactional
    public List<CardResponse> getCardsForCurrentUser(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Klient nie został znaleziony."));

        return cardRepository.findAccessibleByCustomerId(customer.getId()).stream()
                .peek(this::syncStatusFromCardNetwork)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CardResponse getById(UUID cardId, String email) {
        Card card = getAccessibleCard(cardId, email);
        syncStatusFromCardNetwork(card);
        return mapToResponse(card);
    }

    @Transactional
    public IssueCardResponse issue(IssueCardRequest request, String email) {
        if (request.cardType() == CardType.DEBIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dla zewnętrznej sieci kartowej użyj typu VIRTUAL, PHYSICAL albo PREPAID.");
        }

        Account account = accountService.getAccountEntity(request.accountId(), email);
        BigDecimal initialBalance = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();

        CardNetworkIssueResponse networkResponse;
        try {
            networkResponse = cardNetworkClient.issueCard(
                    account.getCustomer().getId(),
                    account.getId(),
                    request.cardType(),
                    initialBalance
            );
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Moduł kart odrzucił żądanie: " + ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Moduł kart jest niedostępny: " + ex.getMessage());
        }

        String currency = account.getBalance().getCurrency();
        Card card = cardRepository.save(Card.builder()
                .account(account)
                .externalCardToken(networkResponse.cardToken())
                .maskedPan(networkResponse.maskedPan())
                .last4(extractLast4(networkResponse.fullPan(), networkResponse.maskedPan()))
                .type(CardType.valueOf(networkResponse.cardType()))
                .status(CardStatus.valueOf(networkResponse.status()))
                .expiresAt(expiryDate(networkResponse.expiryMonth(), networkResponse.expiryYear()))
                .dailyLimit(Money.of(valueOrDefault(request.dailyLimit(), DEFAULT_DAILY_LIMIT), currency))
                .monthlyLimit(Money.of(valueOrDefault(request.monthlyLimit(), DEFAULT_MONTHLY_LIMIT), currency))
                .build());

        return new IssueCardResponse(
                mapToResponse(card),
                networkResponse.fullPan(),
                networkResponse.cvv(),
                networkResponse.message()
        );
    }

    @Transactional
    public CardResponse block(UUID cardId, String email) {
        Card card = getAccessibleCard(cardId, email);
        requireExternalToken(card);

        callCardNetwork(() -> cardNetworkClient.blockCard(card.getExternalCardToken(), "Blocked in bank app"));
        card.setStatus(CardStatus.BLOCKED);
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse unblock(UUID cardId, String email) {
        Card card = getAccessibleCard(cardId, email);
        requireExternalToken(card);

        callCardNetwork(() -> cardNetworkClient.unblockCard(card.getExternalCardToken()));
        card.setStatus(CardStatus.ACTIVE);
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse activate(UUID cardId, String email) {
        Card card = getAccessibleCard(cardId, email);
        requireExternalToken(card);

        syncStatusFromCardNetwork(card);
        if (card.getStatus() == CardStatus.ACTIVE) {
            return mapToResponse(card);
        }
        if (card.getType() == CardType.VIRTUAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Karta wirtualna aktywuje się automatycznie w module kart.");
        }
        if (card.getStatus() != CardStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kartę można aktywować dopiero po wysłaniu przez operatora kart.");
        }

        callCardNetwork(() -> cardNetworkClient.activateCard(card.getExternalCardToken(), email));
        card.setStatus(CardStatus.ACTIVE);
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse updateLimits(UUID cardId, BigDecimal dailyLimit, BigDecimal monthlyLimit, String email) {
        Card card = getAccessibleCard(cardId, email);
        String currency = card.getAccount().getBalance().getCurrency();

        if (dailyLimit != null) {
            requireNonNegative(dailyLimit, "Dzienny limit nie może być ujemny.");
            card.setDailyLimit(Money.of(dailyLimit, currency));
        }
        if (monthlyLimit != null) {
            requireNonNegative(monthlyLimit, "Miesięczny limit nie może być ujemny.");
            card.setMonthlyLimit(Money.of(monthlyLimit, currency));
        }

        return mapToResponse(card);
    }

    private Card getAccessibleCard(UUID cardId, String email) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Karta nie istnieje."));
        accountService.getAccountEntity(card.getAccount().getId(), email);
        return card;
    }

    private void requireExternalToken(Card card) {
        if (card.getExternalCardToken() == null || card.getExternalCardToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Karta nie ma tokenu w zewnętrznej sieci kartowej.");
        }
    }

    private void syncStatusFromCardNetwork(Card card) {
        if (card.getExternalCardToken() == null) {
            return;
        }

        try {
            CardNetworkCardResponse response = cardNetworkClient.getCard(card.getExternalCardToken());
            if (response == null || response.status() == null) {
                return;
            }
            CardStatus externalStatus = CardStatus.valueOf(response.status());
            if (card.getStatus() != externalStatus) {
                card.setStatus(externalStatus);
            }
            if (response.maskedPan() != null && !response.maskedPan().isBlank()) {
                card.setMaskedPan(response.maskedPan());
                card.setLast4(extractLast4(null, response.maskedPan()));
            }
            card.setNetworkBalance(BigDecimal.valueOf(response.balance()));
        } catch (Exception ignored) {
            // Lokalna lista kart nadal ma działać, nawet gdy gateway kart jest chwilowo niedostępny.
        }
    }

    private void callCardNetwork(CardNetworkCall call) {
        try {
            CardNetworkStatusResponse response = call.execute();
            if (response != null && !response.success()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Moduł kart zwrócił błąd: " + response.message());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Moduł kart odrzucił żądanie: " + ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Moduł kart jest niedostępny: " + ex.getMessage());
        }
    }

    private CardResponse mapToResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getAccount().getId(),
                card.getExternalCardToken(),
                card.getMaskedPan(),
                card.getLast4(),
                card.getType(),
                card.getStatus(),
                card.getExpiresAt(),
                card.getDailyLimit() == null ? null : card.getDailyLimit().getAmount(),
                card.getMonthlyLimit() == null ? null : card.getMonthlyLimit().getAmount(),
                card.getNetworkBalance(),
                card.getCreatedAt()
        );
    }

    private LocalDate expiryDate(int month, int year) {
        int fullYear = year < 100 ? 2000 + year : year;
        return LocalDate.of(fullYear, month, 1).withDayOfMonth(LocalDate.of(fullYear, month, 1).lengthOfMonth());
    }

    private String extractLast4(String fullPan, String maskedPan) {
        String source = fullPan != null && !fullPan.isBlank() ? fullPan : maskedPan;
        String digits = source == null ? "" : source.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "0000";
        }
        return digits.substring(digits.length() - 4);
    }

    private BigDecimal valueOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private void requireNonNegative(BigDecimal value, String message) {
        if (value.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    @FunctionalInterface
    private interface CardNetworkCall {
        CardNetworkStatusResponse execute();
    }
}
