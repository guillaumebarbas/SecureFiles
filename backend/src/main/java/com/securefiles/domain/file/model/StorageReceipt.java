package com.securefiles.domain.file.model;

import java.util.Objects;

public record StorageReceipt(String storageKey, String storageVersion) {

    public StorageReceipt {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must not be blank");
        }
        if (storageVersion == null || storageVersion.isBlank()) {
            throw new IllegalArgumentException("storageVersion must not be blank");
        }
        storageKey = storageKey.trim();
        storageVersion = storageVersion.trim();
        Objects.requireNonNull(storageKey);
        Objects.requireNonNull(storageVersion);
    }
}
