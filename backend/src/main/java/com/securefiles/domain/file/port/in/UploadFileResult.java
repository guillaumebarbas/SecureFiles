package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.FileStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UploadFileResult(
        UUID fileId,
        String originalFilename,
        long sizeBytes,
        FileStatus status,
        Instant createdAt) {

    public UploadFileResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
