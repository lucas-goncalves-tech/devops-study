package com.securepay.ledger.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O email não pode ser vazio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "A senha não pode ser vazia")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @NotBlank(message = "O nome completo não pode ser vazio")
        String fullName
) {
}
