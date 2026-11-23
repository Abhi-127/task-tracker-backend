package com.tasktracker.backend.dto;

import com.tasktracker.backend.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        LocalDate dueDate,
        TaskStatus status,
        Long teamId,
        Long assignedToId
) {}
