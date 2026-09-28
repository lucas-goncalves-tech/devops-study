package com.securepay.ledger.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID accountId,
        BigDecimal balance,
        String currency,
        Long version,
        Instant updatedAt
) {
}
