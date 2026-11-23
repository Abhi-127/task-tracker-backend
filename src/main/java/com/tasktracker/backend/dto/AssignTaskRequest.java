package com.tasktracker.backend.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(
        @NotNull(message = "assigneeId is required") Long assigneeId
) {}
