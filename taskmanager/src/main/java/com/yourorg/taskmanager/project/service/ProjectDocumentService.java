package com.yourorg.taskmanager.project.service;

import com.yourorg.taskmanager.auth.entity.SystemRole;
import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.common.exception.AccessDeniedCustomException;
import com.yourorg.taskmanager.common.exception.ResourceNotFoundException;
import com.yourorg.taskmanager.project.dto.DocumentResponse;
import com.yourorg.taskmanager.project.entity.Project;
import com.yourorg.taskmanager.project.entity.ProjectDocument;
import com.yourorg.taskmanager.project.entity.ProjectMember;
import com.yourorg.taskmanager.project.entity.ProjectRole;
import com.yourorg.taskmanager.project.repository.ProjectDocumentRepository;
import com.yourorg.taskmanager.project.repository.ProjectMemberRepository;
import com.yourorg.taskmanager.project.repository.ProjectRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectDocumentService {

    private final ProjectDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectService projectService;
    private final DocumentStorageService storageService;

    public ProjectDocumentService(ProjectDocumentRepository documentRepository,
                                  ProjectRepository projectRepository,
                                  ProjectMemberRepository memberRepository,
                                  ProjectService projectService,
                                  DocumentStorageService storageService) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectService = projectService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(User actor, UUID projectId, String query) {
        projectService.requireMemberOrAdmin(actor, projectId);
        List<ProjectDocument> documents = query == null || query.isBlank()
                ? documentRepository.findAllByProject_IdOrderByUploadedAtDesc(projectId)
                : documentRepository.searchInProject(projectId, query.trim());
        return documents.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse upload(User actor, UUID projectId, MultipartFile file) {
        projectService.requireMemberOrAdmin(actor, projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        DocumentStorageService.StoredUpload stored = storageService.store(file);
        try {
            ProjectDocument document = new ProjectDocument();
            document.setProject(project);
            document.setUploadedBy(actor);
            document.setOriginalFilename(stored.originalFilename());
            document.setStoredFilename(stored.storedFilename());
            document.setMediaType(stored.mediaType());
            document.setFileSize(stored.fileSize());
            document.setExtractedText(stored.extractedText());
            return toResponse(documentRepository.save(document));
        } catch (RuntimeException ex) {
            storageService.delete(stored.storedFilename());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public DownloadDocument download(User actor, UUID projectId, UUID documentId) throws IOException {
        projectService.requireMemberOrAdmin(actor, projectId);
        ProjectDocument document = findDocument(projectId, documentId);
        Path path = storageService.pathFor(document.getStoredFilename());
        Resource resource = new UrlResource(path.toUri());
        return new DownloadDocument(document.getOriginalFilename(), document.getMediaType(), resource);
    }

    @Transactional
    public void delete(User actor, UUID projectId, UUID documentId) {
        projectService.requireMemberOrAdmin(actor, projectId);
        ProjectDocument document = findDocument(projectId, documentId);
        boolean isUploader = document.getUploadedBy().getId().equals(actor.getId());
        boolean isAdmin = actor.getSystemRole() == SystemRole.ADMIN;
        boolean isOwner = memberRepository.findByProject_IdAndUser_Id(projectId, actor.getId())
                .map(ProjectMember::getRoleInProject).orElse(null) == ProjectRole.OWNER;
        if (!isUploader && !isAdmin && !isOwner) {
            throw new AccessDeniedCustomException("Only the uploader, project owner, or admin can delete documents");
        }
        documentRepository.delete(document);
        storageService.delete(document.getStoredFilename());
    }

    private ProjectDocument findDocument(UUID projectId, UUID documentId) {
        return documentRepository.findByIdAndProject_Id(documentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private DocumentResponse toResponse(ProjectDocument document) {
        User uploader = document.getUploadedBy();
        return new DocumentResponse(document.getId(), document.getProject().getId(),
                document.getOriginalFilename(), document.getMediaType(), document.getFileSize(),
                uploader.getId(), uploader.getFullName(), document.getUploadedAt());
    }

    public record DownloadDocument(String filename, String mediaType, Resource resource) {
    }

}