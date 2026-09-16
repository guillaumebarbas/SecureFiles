package com.securefiles.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UploadFileResponseDto(
        UUID fileId,
        String originalFilename,
        long sizeBytes,
        String status,
        Instant createdAt) {

    public UploadFileResponseDto {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}