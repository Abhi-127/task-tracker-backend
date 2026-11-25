package com.tasktracker.backend.dto;

public record AuthResponse(String token, UserResponse user) {}
