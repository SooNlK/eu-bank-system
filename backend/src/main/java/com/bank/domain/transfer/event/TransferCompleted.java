package com.bank.domain.transfer.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransferCompleted(
        UUID transferId,
        LocalDateTime completedAt
) {}
