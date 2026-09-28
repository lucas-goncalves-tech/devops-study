package com.securepay.ledger.payment.dto;

import com.securepay.ledger.domain.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String idempotencyKey,
        UUID sourceWalletId,
        UUID destinationWalletId,
        BigDecimal amount,
        TransactionStatus status,
        String failureReason,
        Instant createdAt
) {
}
