package com.notesapi.notes_api.note.dtos;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageNoteResponse(
        List<NoteResponse> content,
        int page,
        int size,
        Long totalElements,
        int totalPages,
        boolean isLast
) {
    public static PageNoteResponse fromPageable(Page<NoteResponse> notePage) {
        return new PageNoteResponse(
                notePage.getContent(),
                notePage.getNumber(),
                notePage.getSize(),
                notePage.getTotalElements(),
                notePage.getTotalPages(),
                notePage.isLast()
        );

    }
}
