package com.yourorg.taskmanager.auth.controller;

import com.yourorg.taskmanager.auth.dto.AdminUpdateUserRequest;
import com.yourorg.taskmanager.auth.dto.AdminUserResponse;
import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.auth.service.AdminUserService;
import com.yourorg.taskmanager.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list(@AuthenticationPrincipal User actor) {
        return ApiResponse.success(adminUserService.list(actor));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<AdminUserResponse> update(@AuthenticationPrincipal User actor,
            @PathVariable UUID userId, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success(adminUserService.update(actor, userId, request));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal User actor, @PathVariable UUID userId) {
        adminUserService.deactivate(actor, userId);
    }
}