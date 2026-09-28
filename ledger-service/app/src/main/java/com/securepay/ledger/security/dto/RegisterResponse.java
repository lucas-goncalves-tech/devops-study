package com.securepay.ledger.security.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID accountId,
        String email,
        String fullName,
        UUID walletId
) {
}
