package com.tasktracker.backend.dto;

import com.tasktracker.backend.model.Team;
import java.time.LocalDateTime;
import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String description,
        Long createdById,
        String createdByName,
        List<UserResponse> members,
        LocalDateTime createdAt
) {
    public static TeamResponse from(Team team) {
        List<UserResponse> memberList = team.getMembers().stream()
                .map(UserResponse::from)
                .toList();
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedBy().getId(),
                team.getCreatedBy().getName(),
                memberList,
                team.getCreatedAt()
        );
    }
}
