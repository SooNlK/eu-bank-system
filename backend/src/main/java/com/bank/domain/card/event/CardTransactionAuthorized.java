package com.bank.domain.card.event;

import com.bank.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

public record CardTransactionAuthorized(
        UUID cardId,
        UUID accountId,
        Money amount,
        LocalDateTime timestamp
) {}
