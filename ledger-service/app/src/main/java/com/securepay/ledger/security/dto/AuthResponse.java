package com.securepay.ledger.security.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        String tokenType
) {
}
