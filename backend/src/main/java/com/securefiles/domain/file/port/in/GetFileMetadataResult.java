package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.FileStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record GetFileMetadataResult(
        UUID fileId,
        String originalFilename,
    String author,
        Optional<Long> sizeBytes,
        FileStatus status,
        Instant createdAt,
        Optional<String> failureCode,
        Optional<String> failureCause,
        boolean canDownload,
        boolean canDelete) {

    public GetFileMetadataResult(
            UUID fileId,
            String originalFilename,
            String author,
            Optional<Long> sizeBytes,
            FileStatus status,
            Instant createdAt,
            Optional<String> failureCode) {
        this(fileId, originalFilename, author, sizeBytes, status, createdAt, failureCode, Optional.empty(), false, false);
    }

    public GetFileMetadataResult(
            UUID fileId,
            String originalFilename,
            String author,
            Optional<Long> sizeBytes,
            FileStatus status,
            Instant createdAt,
            Optional<String> failureCode,
            Optional<String> failureCause) {
        this(fileId, originalFilename, author, sizeBytes, status, createdAt, failureCode, failureCause, false, false);
    }

    public GetFileMetadataResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        Objects.requireNonNull(author, "author must not be null");
        Objects.requireNonNull(sizeBytes, "sizeBytes must not be null");
        sizeBytes.ifPresent(size -> {
            if (size < 0) {
                throw new IllegalArgumentException("sizeBytes must not be negative");
            }
        });
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        failureCode.ifPresent(code -> {
            if (code.isBlank()) {
                throw new IllegalArgumentException("failureCode must not be blank");
            }
        });
        Objects.requireNonNull(failureCause, "failureCause must not be null");
        failureCause.ifPresent(cause -> {
            if (cause.isBlank()) {
                throw new IllegalArgumentException("failureCause must not be blank");
            }
        });
    }
}