package com.securefiles.domain.file.port.in;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record DownloadFileResult(
        UUID fileId,
        String originalFilename,
        Optional<String> contentType,
        long sizeBytes,
        InputStream content) {

    public DownloadFileResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(content, "content must not be null");
    }
}
