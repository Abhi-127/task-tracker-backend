package com.tasktracker.backend.dto;

import com.tasktracker.backend.model.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String bio,
        String role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBio(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
