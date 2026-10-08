package com.notesapi.notes_api.note.dtos;

import com.notesapi.notes_api.note.entities.Note;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String title,
        String content,
        boolean completed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteResponse fromEntity(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.isCompleted(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
