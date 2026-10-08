package com.notesapi.notes_api.auth.dtos;

import com.notesapi.notes_api.auth.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@FieldMatch(field = "password", fieldMatch = "confirmPassword", message = "As senhas devem ser iguais!")
public record RegisterRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato do email é inválido")
        @Size(max = 150, message = "Campo email deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "Nome é obrigatorio")
        @Size(min = 3, message = "Nome precisa ter no mínimo 3 caracteres")
        String displayName,

        @NotBlank(message = "Senha é obrigatório")
        @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres")
        String password,
        @NotBlank(message = "Confirmação de senha é obrigatório")
        @Size(min = 8, message = "Confirmação de senha deve ter no minimo 8 caracteres")
        String confirmPassword
) {

    public RegisterRequest {
        email = email != null ? email.trim().toLowerCase() : null;
        displayName = displayName != null ? displayName.trim() : null;
    }
}
