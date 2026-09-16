package com.securefiles.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FileMetadataResponseDto(
        UUID fileId,
        String originalFilename,
        Long sizeBytes,
        String status,
        Instant createdAt,
        String failureCode) {

    public FileMetadataResponseDto {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        if (sizeBytes != null && sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (failureCode != null && failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
    }
}