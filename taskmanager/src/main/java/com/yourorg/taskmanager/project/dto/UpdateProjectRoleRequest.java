package com.yourorg.taskmanager.project.dto;

import com.yourorg.taskmanager.project.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectRoleRequest(@NotNull ProjectRole projectRole) {
}