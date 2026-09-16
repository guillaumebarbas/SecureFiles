package com.securefiles.domain.file.model;

import java.util.Objects;

public record StorageMetadata(long sizeBytes, String storageVersion) {

    public StorageMetadata {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        if (storageVersion == null || storageVersion.isBlank()) {
            throw new IllegalArgumentException("storageVersion must not be blank");
        }
        storageVersion = storageVersion.trim();
        Objects.requireNonNull(storageVersion);
    }
}
