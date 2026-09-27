package com.yourorg.taskmanager.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        List<MemberResponse> members) {

    public record MemberResponse(UUID userId, String email, String fullName,
                                 String systemRole, String projectRole, Instant joinedAt) {
    }
}