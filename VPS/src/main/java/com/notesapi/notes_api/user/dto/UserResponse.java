package com.notesapi.notes_api.user.dto;

import com.notesapi.notes_api.user.entities.User;

import java.time.LocalDateTime;

public record UserResponse(
        String email,
        String displayName,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt());

    }
}
