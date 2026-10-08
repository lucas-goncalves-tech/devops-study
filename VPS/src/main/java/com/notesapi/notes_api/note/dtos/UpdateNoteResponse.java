package com.notesapi.notes_api.note.dtos;

public record UpdateNoteResponse(
        String message,
        NoteResponse data
) {
}
