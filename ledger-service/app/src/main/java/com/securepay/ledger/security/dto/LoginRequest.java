package com.securepay.ledger.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O email não pode ser vazio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "A senha não pode ser vazia")
        String password
) {
}
