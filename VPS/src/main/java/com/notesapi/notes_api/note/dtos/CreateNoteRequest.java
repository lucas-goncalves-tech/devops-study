package com.notesapi.notes_api.note.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank(message = "Titulo é obrigatório")
        @Size(min = 4, message = "Titulo deve ser no minimo 4 characteres")
        String title,

        @NotBlank(message = "Conteudo é obrigatório")
        @Size(min = 10, message = "Conteudo deve ter no minimo 10 caracteres")
        String content
) {
    public CreateNoteRequest {
        title = title != null ? title.trim() : null;
        content = content != null ? content.trim() : null;
    }

}
