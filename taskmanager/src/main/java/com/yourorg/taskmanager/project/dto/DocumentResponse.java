package com.yourorg.taskmanager.project.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(UUID id, UUID projectId, String filename, String mediaType,
                              long sizeBytes, UUID uploadedById, String uploadedByName,
                              Instant uploadedAt) {
}