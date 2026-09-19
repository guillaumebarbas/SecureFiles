package com.securefiles.domain.file.model;

import java.util.Objects;

public record StorageQuota(String ownerId, long usedBytes, long quotaBytes) {

    public StorageQuota {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        if (usedBytes < 0) {
            throw new IllegalArgumentException("usedBytes must not be negative");
        }
        if (quotaBytes <= 0) {
            throw new IllegalArgumentException("quotaBytes must be positive");
        }
        Objects.requireNonNull(ownerId, "ownerId must not be null");
    }
}