package com.tasktracker.backend.dto;

import com.tasktracker.backend.model.Task;
import com.tasktracker.backend.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Long teamId,
        String teamName,
        Long createdById,
        String createdByName,
        Long assignedToId,
        String assignedToName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getTeam() != null ? task.getTeam().getId() : null,
                task.getTeam() != null ? task.getTeam().getName() : null,
                task.getCreatedBy().getId(),
                task.getCreatedBy().getName(),
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getName() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
