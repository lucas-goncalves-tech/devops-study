package com.notesapi.notes_api.note.dtos;

public record CreateNoteResponse(
        String message,
        NoteResponse data
) {
}
