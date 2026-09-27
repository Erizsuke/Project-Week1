package com.yourorg.taskmanager.auth.dto;

import com.yourorg.taskmanager.auth.entity.User;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String fullName, String systemRole,
                                boolean active, Instant createdAt) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getSystemRole().name(), user.isActive(), user.getCreatedAt());
    }
}