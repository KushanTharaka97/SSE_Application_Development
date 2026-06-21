package com.govtech.gsrp_backend.application.service;

import com.govtech.gsrp_backend.application.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for storing and removing uploaded files on the local file system.
 * The root upload directory is configured via {@code app.file.upload-dir} in application.yaml
 * (backed by the FILE_UPLOAD_DIR environment variable).
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    /**
     * Initialises the upload directory on application startup.
     * Creates the directory (and any missing parents) if it does not already exist.
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("File upload directory initialised at: {}", uploadPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create file upload directory: " + uploadDir, ex);
        }
    }

    /**
     * Stores the given {@link MultipartFile} on disk with a UUID-prefixed filename
     * to avoid collisions and directory-traversal attacks.
     *
     * @param file the incoming multipart file
     * @return the relative path (from upload root) stored in the database, e.g.
     *         {@code documents/550e8400-e29b-41d4-a716-446655440000_MyID.pdf}
     */
    public String storeFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename(), "Filename must not be null"));

        if (originalFilename.contains("..")) {
            throw new BusinessException("Filename contains invalid path sequence: " + originalFilename);
        }

        if (file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty.");
        }

        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
        Path targetLocation = uploadPath.resolve(uniqueFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully: {}", targetLocation);
        } catch (IOException ex) {
            throw new BusinessException("Could not store file '" + originalFilename + "'. Please try again.");
        }

        // Return the relative path that is persisted in the database
        return uniqueFilename;
    }

    /**
     * Deletes a previously stored file identified by its relative filename.
     * If the file does not exist the deletion is silently skipped (idempotent).
     *
     * @param relativeFilename the filename value as stored in the database
     */
    public void deleteFile(String relativeFilename) {
        if (relativeFilename == null || relativeFilename.isBlank()) {
            return;
        }
        Path filePath = uploadPath.resolve(relativeFilename).normalize();
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted from storage: {}", filePath);
            } else {
                log.warn("File not found on disk during delete (skipped): {}", filePath);
            }
        } catch (IOException ex) {
            log.error("Could not delete file '{}': {}", filePath, ex.getMessage());
        }
    }

    /**
     * Returns the absolute {@link Path} for a stored file (useful for streaming downloads).
     *
     * @param relativeFilename the filename value as stored in the database
     * @return resolved absolute {@link Path}
     */
    public Path resolveFilePath(String relativeFilename) {
        return uploadPath.resolve(relativeFilename).normalize();
    }
}
