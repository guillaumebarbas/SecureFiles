package com.securefiles.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

public record ScanFileCommand(
        UUID eventId,
        UUID fileId,
        Long expectedSizeBytes,
        String expectedSha256,
        String expectedStorageKey,
        String expectedStorageVersion) {

    public ScanFileCommand(UUID fileId) {
        this(null, fileId, null, null, null, null);
    }

    public ScanFileCommand {
        Objects.requireNonNull(fileId, "fileId must not be null");
        if (expectedSizeBytes != null && expectedSizeBytes < 0) {
            throw new IllegalArgumentException("expectedSizeBytes must not be negative");
        }
        boolean hasExpectedMetadata = expectedSizeBytes != null
                || expectedSha256 != null
                || expectedStorageKey != null
                || expectedStorageVersion != null;
        if (hasExpectedMetadata
                && (expectedSizeBytes == null
                || expectedSha256 == null
                || expectedStorageKey == null
                || expectedStorageVersion == null)) {
            throw new IllegalArgumentException("expected storage metadata must be complete");
        }
    }
}
