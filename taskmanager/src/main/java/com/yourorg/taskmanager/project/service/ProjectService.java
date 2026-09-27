package com.yourorg.taskmanager.project.service;

import com.yourorg.taskmanager.auth.entity.SystemRole;
import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.auth.repository.UserRepository;
import com.yourorg.taskmanager.common.exception.AccessDeniedCustomException;
import com.yourorg.taskmanager.common.exception.BadRequestException;
import com.yourorg.taskmanager.common.exception.DuplicateResourceException;
import com.yourorg.taskmanager.common.exception.ResourceNotFoundException;
import com.yourorg.taskmanager.project.dto.AddProjectMemberRequest;
import com.yourorg.taskmanager.project.dto.ProjectRequest;
import com.yourorg.taskmanager.project.dto.ProjectResponse;
import com.yourorg.taskmanager.project.dto.UpdateProjectRoleRequest;
import com.yourorg.taskmanager.project.entity.Project;
import com.yourorg.taskmanager.project.entity.ProjectMember;
import com.yourorg.taskmanager.project.entity.ProjectRole;
import com.yourorg.taskmanager.project.repository.ProjectMemberRepository;
import com.yourorg.taskmanager.project.repository.ProjectDocumentRepository;
import com.yourorg.taskmanager.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectDocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          UserRepository userRepository,
                          ProjectDocumentRepository documentRepository,
                          DocumentStorageService documentStorageService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(User actor) {
        List<Project> projects = actor.getSystemRole() == SystemRole.ADMIN
                ? projectRepository.findAll()
                : memberRepository.findByUser_Id(actor.getId()).stream()
                        .map(ProjectMember::getProject).toList();
        return projects.stream()
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse create(User actor, ProjectRequest request) {
        requireProjectCreator(actor);
        Project project = new Project();
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setCreatedBy(actor);
        project = projectRepository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProject(project);
        owner.setUser(actor);
        owner.setRoleInProject(ProjectRole.OWNER);
        memberRepository.save(owner);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(User actor, UUID projectId) {
        Project project = findProject(projectId);
        requireMemberOrAdmin(actor, projectId);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(User actor, UUID projectId, ProjectRequest request) {
        Project project = findProject(projectId);
        requireProjectManager(actor, projectId);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(User actor, UUID projectId) {
        Project project = findProject(projectId);
        requireProjectManager(actor, projectId);
        documentRepository.findAllByProject_IdOrderByUploadedAtDesc(projectId)
            .forEach(document -> documentStorageService.delete(document.getStoredFilename()));
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse.MemberResponse> listMembers(User actor, UUID projectId) {
        findProject(projectId);
        requireMemberOrAdmin(actor, projectId);
        return memberRepository.findByProject_Id(projectId).stream()
                .map(this::toMemberResponse).toList();
    }

    @Transactional
    public ProjectResponse.MemberResponse addMember(User actor, UUID projectId,
                                                     AddProjectMemberRequest request) {
        findProject(projectId);
        requireProjectManager(actor, projectId);
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found; invite registered users only"));
        if (memberRepository.existsByProject_IdAndUser_Id(projectId, user.getId())) {
            throw new DuplicateResourceException("User is already a project member");
        }
        ProjectMember member = new ProjectMember();
        member.setProject(findProject(projectId));
        member.setUser(user);
        member.setRoleInProject(ProjectRole.MEMBER);
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public void removeMember(User actor, UUID projectId, UUID userId) {
        findProject(projectId);
        requireProjectManager(actor, projectId);
        ProjectMember member = memberRepository.findByProject_IdAndUser_Id(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));
        if (member.getRoleInProject() == ProjectRole.OWNER) {
            throw new BadRequestException("Transfer project ownership before removing the owner");
        }
        memberRepository.delete(member);
    }

    @Transactional
    public ProjectResponse.MemberResponse updateMemberRole(User actor, UUID projectId, UUID userId,
                                                           UpdateProjectRoleRequest request) {
        if (actor.getSystemRole() != SystemRole.ADMIN) {
            throw new AccessDeniedCustomException("Admin role is required to change project roles");
        }
        findProject(projectId);
        ProjectMember member = memberRepository.findByProject_IdAndUser_Id(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));
        if (member.getRoleInProject() == ProjectRole.OWNER
                && request.projectRole() != ProjectRole.OWNER
                && memberRepository.countByProject_IdAndRoleInProject(projectId, ProjectRole.OWNER) <= 1) {
            throw new BadRequestException("Promote another member to owner before changing the last owner");
        }
        member.setRoleInProject(request.projectRole());
        return toMemberResponse(memberRepository.save(member));
    }

    public void requireMemberOrAdmin(User actor, UUID projectId) {
        if (actor.getSystemRole() != SystemRole.ADMIN
                && !memberRepository.existsByProject_IdAndUser_Id(projectId, actor.getId())) {
            throw new AccessDeniedCustomException("You are not a member of this project");
        }
    }

    public void requireProjectManager(User actor, UUID projectId) {
        if (actor.getSystemRole() == SystemRole.ADMIN) {
            return;
        }
        ProjectMember member = memberRepository.findByProject_IdAndUser_Id(projectId, actor.getId())
                .orElseThrow(() -> new AccessDeniedCustomException("You are not a member of this project"));
        if (member.getRoleInProject() != ProjectRole.OWNER) {
            throw new AccessDeniedCustomException("Project owner or admin access is required");
        }
    }

    private void requireProjectCreator(User actor) {
        if (actor.getSystemRole() != SystemRole.OWNER && actor.getSystemRole() != SystemRole.ADMIN) {
            throw new AccessDeniedCustomException("Owner or admin role is required to create projects");
        }
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectResponse toResponse(Project project) {
        List<ProjectResponse.MemberResponse> members = memberRepository.findByProject_Id(project.getId()).stream()
                .map(this::toMemberResponse).toList();
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getCreatedBy().getId(), project.getCreatedBy().getFullName(),
                project.getCreatedAt(), members);
    }

    private ProjectResponse.MemberResponse toMemberResponse(ProjectMember member) {
        User user = member.getUser();
        return new ProjectResponse.MemberResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getSystemRole().name(), member.getRoleInProject().name(), member.getJoinedAt());
    }
}