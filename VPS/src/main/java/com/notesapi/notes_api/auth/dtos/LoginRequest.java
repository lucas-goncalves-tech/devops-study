package com.notesapi.notes_api.auth.dtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato do email é inválido")
        @Size(max = 150, message = "Campo email deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatório")
        @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres")
        String password
) {
    public LoginRequest {
        email = email != null ? email.trim().toLowerCase() : null;
    }

}
