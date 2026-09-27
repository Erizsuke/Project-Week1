package com.yourorg.taskmanager.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description) {
}