package com.yourorg.taskmanager.project.controller;

import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.common.dto.ApiResponse;
import com.yourorg.taskmanager.project.dto.DocumentResponse;
import com.yourorg.taskmanager.project.service.ProjectDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectDocumentController {

    private final ProjectDocumentService documentService;

    public ProjectDocumentController(ProjectDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/documents")
    public ApiResponse<List<DocumentResponse>> list(@AuthenticationPrincipal User actor,
            @PathVariable UUID projectId, @RequestParam(required = false) String q) {
        return ApiResponse.success(documentService.list(actor, projectId, q));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(@AuthenticationPrincipal User actor,
            @PathVariable UUID projectId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(actor, projectId, file));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User actor,
            @PathVariable UUID projectId, @PathVariable UUID documentId) throws IOException {
        ProjectDocumentService.DownloadDocument document =
                documentService.download(actor, projectId, documentId);
        MediaType mediaType = document.mediaType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(document.mediaType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.filename(), StandardCharsets.UTF_8).build().toString())
                .body(document.resource());
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User actor, @PathVariable UUID projectId,
                       @PathVariable UUID documentId) {
        documentService.delete(actor, projectId, documentId);
    }

}