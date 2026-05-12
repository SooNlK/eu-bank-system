package com.bank.domain.transfer.event;

import com.bank.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransferInitiated(
        UUID transferId,
        UUID fromAccountId,
        UUID toAccountId,
        Money amount,
        LocalDateTime timestamp
) {}
