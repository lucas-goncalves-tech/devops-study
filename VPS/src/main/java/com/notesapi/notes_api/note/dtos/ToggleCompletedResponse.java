package com.notesapi.notes_api.note.dtos;

public record ToggleCompletedResponse(
        String message,
        NoteResponse data
) {
}
