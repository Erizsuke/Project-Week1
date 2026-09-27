package com.yourorg.taskmanager.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddProjectMemberRequest(@NotBlank @Email String email) {
}