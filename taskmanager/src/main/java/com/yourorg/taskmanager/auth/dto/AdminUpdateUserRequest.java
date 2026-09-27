package com.yourorg.taskmanager.auth.dto;

import com.yourorg.taskmanager.auth.entity.SystemRole;

public record AdminUpdateUserRequest(SystemRole systemRole) {
}