package com.yourorg.taskmanager.project.controller;

import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.common.dto.ApiResponse;
import com.yourorg.taskmanager.project.dto.AddProjectMemberRequest;
import com.yourorg.taskmanager.project.dto.ProjectRequest;
import com.yourorg.taskmanager.project.dto.ProjectResponse;
import com.yourorg.taskmanager.project.dto.UpdateProjectRoleRequest;
import com.yourorg.taskmanager.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(@AuthenticationPrincipal User actor) {
        return ApiResponse.success(projectService.list(actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> create(@AuthenticationPrincipal User actor,
                                                @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(projectService.create(actor, request));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@AuthenticationPrincipal User actor,
                                            @PathVariable UUID projectId) {
        return ApiResponse.success(projectService.get(actor, projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(@AuthenticationPrincipal User actor,
                                               @PathVariable UUID projectId,
                                               @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(projectService.update(actor, projectId, request));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User actor, @PathVariable UUID projectId) {
        projectService.delete(actor, projectId);
    }

    @GetMapping("/{projectId}/members")
    public ApiResponse<List<ProjectResponse.MemberResponse>> listMembers(
            @AuthenticationPrincipal User actor, @PathVariable UUID projectId) {
        return ApiResponse.success(projectService.listMembers(actor, projectId));
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse.MemberResponse> addMember(
            @AuthenticationPrincipal User actor, @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        return ApiResponse.success(projectService.addMember(actor, projectId, request));
    }

    @PatchMapping("/{projectId}/members/{userId}")
    public ApiResponse<ProjectResponse.MemberResponse> updateMemberRole(
            @AuthenticationPrincipal User actor, @PathVariable UUID projectId,
            @PathVariable UUID userId, @Valid @RequestBody UpdateProjectRoleRequest request) {
        return ApiResponse.success(projectService.updateMemberRole(actor, projectId, userId, request));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal User actor, @PathVariable UUID projectId,
                             @PathVariable UUID userId) {
        projectService.removeMember(actor, projectId, userId);
    }
}