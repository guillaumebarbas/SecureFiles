package com.securefiles.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FileMetadataResponseDto(
        UUID fileId,
        String originalFilename,
    String author,
        Long sizeBytes,
        String status,
        Instant createdAt,
        String failureCode,
        String failureCause) {

    public FileMetadataResponseDto(
            UUID fileId,
            String originalFilename,
            String author,
            Long sizeBytes,
            String status,
            Instant createdAt,
            String failureCode) {
        this(fileId, originalFilename, author, sizeBytes, status, createdAt, failureCode, null);
    }

    public FileMetadataResponseDto {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        Objects.requireNonNull(author, "author must not be null");
        if (sizeBytes != null && sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (failureCode != null && failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
        if (failureCause != null && failureCause.isBlank()) {
            throw new IllegalArgumentException("failureCause must not be blank");
        }
    }
}