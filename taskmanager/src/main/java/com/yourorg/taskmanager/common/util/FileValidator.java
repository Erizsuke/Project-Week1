package com.yourorg.taskmanager.common.util;

import com.yourorg.taskmanager.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class FileValidator {

    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/csv",
            "text/markdown",
            "text/plain"
    );

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/svg+xml", "image/bmp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/x-msvideo"
    );

    private final long maxDocumentSize;
    private final long maxImageSize;
    private final long maxVideoSize;

    public FileValidator(
            @Value("${app.upload.max-document-size:20MB}") DataSize maxDocumentSize,
            @Value("${app.upload.max-image-size:10MB}") DataSize maxImageSize,
            @Value("${app.upload.max-video-size:300MB}") DataSize maxVideoSize) {
        this.maxDocumentSize = maxDocumentSize.toBytes();
        this.maxImageSize = maxImageSize.toBytes();
        this.maxVideoSize = maxVideoSize.toBytes();
    }

    public void validate(MultipartFile file, String contentType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File khong duoc de trong");
        }

        long size = file.getSize();

        if (contentType != null && ALLOWED_DOC_TYPES.contains(contentType)) {
            checkSize(size, maxDocumentSize, "Document");
        } else if (contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType)) {
            checkSize(size, maxImageSize, "Image");
        } else if (contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType)) {
            checkSize(size, maxVideoSize, "Video");
        } else {
            throw new BadRequestException("Dinh dang file khong duoc ho tro: " + contentType);
        }
    }

    private static void checkSize(long size, long max, String type) {
        if (size > max) {
            throw new BadRequestException(type + " vuot qua dung luong cho phep");
        }
    }
}
