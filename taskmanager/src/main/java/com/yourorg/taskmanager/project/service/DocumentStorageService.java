package com.yourorg.taskmanager.project.service;

import org.apache.tika.Tika;
import com.yourorg.taskmanager.common.exception.BadRequestException;
import com.yourorg.taskmanager.common.exception.ResourceNotFoundException;
import com.yourorg.taskmanager.common.util.FileValidator;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path root;
    private final FileValidator fileValidator;
    private final Tika tika = new Tika();

    public DocumentStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir,
                                  FileValidator fileValidator) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileValidator = fileValidator;
    }

    public StoredUpload store(MultipartFile file) {
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        try (InputStream input = file.getInputStream()) {
            String mediaType = tika.detect(input, originalFilename);
            fileValidator.validate(file, mediaType);
            String extractedText;
            try (InputStream parserInput = file.getInputStream()) {
                extractedText = tika.parseToString(parserInput);
            } catch (TikaException ex) {
                throw new BadRequestException("Unable to read text from this document");
            }

            String storedFilename = UUID.randomUUID() + extensionOf(originalFilename);
            Files.createDirectories(root);
            Path destination = root.resolve(storedFilename).normalize();
            if (!destination.startsWith(root)) {
                throw new BadRequestException("Invalid filename");
            }
            try (InputStream content = file.getInputStream()) {
                Files.copy(content, destination);
            }
            return new StoredUpload(originalFilename, storedFilename, mediaType,
                    file.getSize(), extractedText == null ? "" : extractedText);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store uploaded document");
        }
    }

    public Path pathFor(String storedFilename) {
        Path path = resolveStoredPath(storedFilename);
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Document file not found");
        }
        return path;
    }

    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(resolveStoredPath(storedFilename));
        } catch (IOException ex) {
            throw new BadRequestException("Unable to remove stored document");
        }
    }

    private Path resolveStoredPath(String storedFilename) {
        if (storedFilename == null || storedFilename.contains("/") || storedFilename.contains("\\")) {
            throw new BadRequestException("Invalid stored filename");
        }
        Path path = root.resolve(storedFilename).normalize();
        if (!path.startsWith(root)) {
            throw new BadRequestException("Invalid stored filename");
        }
        return path;
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Filename is required");
        }
        String normalized = filename.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (name.isBlank() || name.length() > 500 || name.contains("..")) {
            throw new BadRequestException("Invalid filename");
        }
        return name;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String extension = filename.substring(dot).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    public record StoredUpload(String originalFilename, String storedFilename, String mediaType,
                               long fileSize, String extractedText) {
    }
}