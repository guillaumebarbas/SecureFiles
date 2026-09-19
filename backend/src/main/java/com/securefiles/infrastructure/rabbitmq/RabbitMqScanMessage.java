package com.securefiles.infrastructure.rabbitmq;

import java.util.Objects;
import java.util.UUID;

public record RabbitMqScanMessage(
        UUID eventId,
        UUID fileId,
        long sizeBytes,
        String sha256,
        String storageKey,
        String storageVersion,
        int publicationAttempt) {

    public RabbitMqScanMessage {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(fileId, "fileId must not be null");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256 must be a lowercase SHA-256 value");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must not be blank");
        }
        if (storageVersion == null || storageVersion.isBlank()) {
            throw new IllegalArgumentException("storageVersion must not be blank");
        }
        if (publicationAttempt < 1) {
            throw new IllegalArgumentException("publicationAttempt must be positive");
        }
    }
}
