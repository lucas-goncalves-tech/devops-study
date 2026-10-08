package com.notesapi.notes_api.note.dtos;

import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
        @Size(min = 1, max = 255, message = "Titulo deve conter entre 1 e 255 caracteres")
        String title,
        @Size(min = 1, max = 255, message = "Conteudo deve conter entre 1 e 255 caracteres")
        String content
) {
    public UpdateNoteRequest {
        title = title != null ? title.trim() : null;
        content = content != null ? content.trim() : null;
    }
}
