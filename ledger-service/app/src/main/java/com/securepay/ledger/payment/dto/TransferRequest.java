package com.securepay.ledger.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "A carteira de destino é obrigatória")
        UUID destinationWalletId,

        @NotNull(message = "O valor da transferência é obrigatório")
        @Positive(message = "O valor da transferência deve ser maior que zero")
        BigDecimal amount
) {
}
