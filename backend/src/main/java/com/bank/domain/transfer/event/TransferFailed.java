package com.bank.domain.transfer.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransferFailed(
        UUID transferId,
        String reason,
        LocalDateTime failedAt
) {}
