package com.lawfirm.application.service;

import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "text/plain"
    );

    private static final Set<String> INLINE_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    private final Path storageLocation;
    private final long maxBytes;

    public DocumentStorageService(
        @Value("${app.storage.documents-path:${user.home}/lawfirm-documents}") String path,
        @Value("${app.storage.max-file-size-mb:20}") int maxMb
    ) {
        this.storageLocation = Paths.get(path).toAbsolutePath().normalize();
        this.maxBytes = (long) maxMb * 1024 * 1024;
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create document storage directory: " + path, e);
        }
    }

    public String store(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                "File size " + file.getSize() + " exceeds maximum of " + (maxBytes / 1024 / 1024) + " MB"
            );
        }
        String ext = extractExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString() + ext;
        try {
            Files.copy(file.getInputStream(), storageLocation.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        return storedName;
    }

    public Resource load(String storedFilename) {
        Path path = storageLocation.resolve(storedFilename).normalize();
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found: " + storedFilename);
        }
        return resource;
    }

    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(storageLocation.resolve(storedFilename));
        } catch (IOException ignored) {
            // file may already be gone
        }
    }

    public boolean isInlinePreviewable(String contentType) {
        return contentType != null && INLINE_TYPES.contains(contentType);
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
