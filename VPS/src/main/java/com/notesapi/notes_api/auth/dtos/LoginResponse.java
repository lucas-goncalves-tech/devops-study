package com.notesapi.notes_api.auth.dtos;

public record LoginResponse(
        String message,
        String token
) {
}
