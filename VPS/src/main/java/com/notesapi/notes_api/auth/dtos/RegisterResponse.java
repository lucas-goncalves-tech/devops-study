package com.notesapi.notes_api.auth.dtos;

import com.notesapi.notes_api.user.dto.UserResponse;

public record RegisterResponse(
        String message,
        UserResponse data
) {
}
