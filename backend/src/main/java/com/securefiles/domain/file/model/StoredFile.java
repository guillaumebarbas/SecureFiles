package com.securefiles.domain.file.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class StoredFile {

    private final UUID id;
    private final String ownerId;
    private final String originalFilename;
    private final String clientContentType;
    private final FileStatus status;
    private final Long sizeBytes;
    private final String sha256;
    private final String storageKey;
    private final String storageVersion;
    private final int scanAttemptCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String failureCode;
    private final UUID scanLeaseId;
    private final Instant scanLeaseUntil;
    private final Instant nextScanAt;

    private StoredFile(
            UUID id,
            String ownerId,
            String originalFilename,
            String clientContentType,
            FileStatus status,
            Long sizeBytes,
            String sha256,
            String storageKey,
            String storageVersion,
            int scanAttemptCount,
            Instant createdAt,
            Instant updatedAt,
            String failureCode,
            UUID scanLeaseId,
            Instant scanLeaseUntil,
            Instant nextScanAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireText(ownerId, "ownerId");
        this.originalFilename = requireText(originalFilename, "originalFilename");
        this.clientContentType = clientContentType;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.storageKey = storageKey;
        this.storageVersion = storageVersion;
        if (scanAttemptCount < 0) {
            throw new IllegalArgumentException("scanAttemptCount must not be negative");
        }
        this.scanAttemptCount = scanAttemptCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.failureCode = failureCode;
        this.scanLeaseId = scanLeaseId;
        this.scanLeaseUntil = scanLeaseUntil;
        this.nextScanAt = nextScanAt;
    }

    public static StoredFile startUpload(
            UUID id,
            String ownerId,
            String originalFilename,
            String clientContentType,
            Instant createdAt) {
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                FileStatus.UPLOADING,
                null,
                null,
                null,
                null,
                0,
                createdAt,
                createdAt,
                null,
                null,
                null,
                null);
    }

    public static StoredFile restore(
            UUID id,
            String ownerId,
            String originalFilename,
            String clientContentType,
            FileStatus status,
            Long sizeBytes,
            String sha256,
            String storageKey,
            String storageVersion,
            int scanAttemptCount,
            Instant createdAt,
            Instant updatedAt,
            String failureCode,
            UUID scanLeaseId,
            Instant scanLeaseUntil,
            Instant nextScanAt) {
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                status,
                sizeBytes,
                sha256,
                storageKey,
                storageVersion,
                scanAttemptCount,
                createdAt,
                updatedAt,
                failureCode,
                scanLeaseId,
                scanLeaseUntil,
                nextScanAt);
    }

    public StoredFile completeUpload(
            long completedSizeBytes,
            String completedSha256,
            StorageReceipt storageReceipt,
            Instant completedAt) {
        if (status != FileStatus.UPLOADING) {
            throw new IllegalStateException("Only an uploading file can be completed");
        }
        if (completedSizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        if (completedSha256 == null || !completedSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256 must be a lowercase SHA-256 value");
        }
        Objects.requireNonNull(storageReceipt, "storageReceipt must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                FileStatus.PENDING_SCAN,
                completedSizeBytes,
                completedSha256,
                storageReceipt.storageKey(),
                storageReceipt.storageVersion(),
                scanAttemptCount,
                createdAt,
                completedAt,
                null,
                null,
                null,
                null);
    }

    public StoredFile claimForScan(UUID leaseId, Instant leaseUntil, Instant claimedAt) {
        if (status != FileStatus.PENDING_SCAN) {
            throw new IllegalStateException("Only a pending scan can be claimed");
        }
        Objects.requireNonNull(leaseId, "leaseId must not be null");
        Objects.requireNonNull(leaseUntil, "leaseUntil must not be null");
        Objects.requireNonNull(claimedAt, "claimedAt must not be null");
        if (!leaseUntil.isAfter(claimedAt)) {
            throw new IllegalArgumentException("leaseUntil must be after claimedAt");
        }
        if (nextScanAt != null && nextScanAt.isAfter(claimedAt)) {
            throw new IllegalStateException("The next scan is not due yet");
        }
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                FileStatus.SCANNING,
                sizeBytes,
                sha256,
                storageKey,
                storageVersion,
                scanAttemptCount + 1,
                createdAt,
                claimedAt,
                null,
                leaseId,
                leaseUntil,
                null);
    }

    public StoredFile completeScan(
            UUID leaseId,
            AntivirusScanResult scanResult,
            Instant completedAt) {
        return completeScan(leaseId, scanResult, completedAt, completedAt);
    }

    public StoredFile completeScan(
            UUID leaseId,
            AntivirusScanResult scanResult,
            Instant completedAt,
            Instant nextScanAt) {
        if (status != FileStatus.SCANNING) {
            throw new IllegalStateException("Only a scanning file can receive a scan result");
        }
        if (!Objects.equals(scanLeaseId, Objects.requireNonNull(leaseId, "leaseId must not be null"))) {
            throw new IllegalStateException("The scan lease does not match");
        }
        Objects.requireNonNull(scanResult, "scanResult must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");

        FileStatus completedStatus = switch (scanResult.verdict()) {
            case CLEAN -> FileStatus.CLEAN;
            case INFECTED -> FileStatus.INFECTED;
            case RETRYABLE_FAILURE -> FileStatus.PENDING_SCAN;
            case TERMINAL_FAILURE -> FileStatus.SCAN_FAILED;
        };
        Instant retryAt = scanResult.verdict() == AntivirusVerdict.RETRYABLE_FAILURE
                ? Objects.requireNonNull(nextScanAt, "nextScanAt must not be null")
                : null;
        String completedFailureCode = scanResult.verdict() == AntivirusVerdict.CLEAN
                || scanResult.verdict() == AntivirusVerdict.INFECTED
                ? null
                : scanResult.failureCode();

        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                completedStatus,
                sizeBytes,
                sha256,
                storageKey,
                storageVersion,
                scanAttemptCount,
                createdAt,
                completedAt,
                completedFailureCode,
                null,
                null,
                retryAt);
    }

    public StoredFile recoverExpiredScan(Instant recoveredAt, Instant nextScanAt) {
        if (status != FileStatus.SCANNING) {
            throw new IllegalStateException("Only a scanning file can recover an expired lease");
        }
        Objects.requireNonNull(recoveredAt, "recoveredAt must not be null");
        Objects.requireNonNull(nextScanAt, "nextScanAt must not be null");
        if (scanLeaseUntil == null || scanLeaseUntil.isAfter(recoveredAt)) {
            throw new IllegalStateException("The scan lease has not expired");
        }
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                FileStatus.PENDING_SCAN,
                sizeBytes,
                sha256,
                storageKey,
                storageVersion,
                scanAttemptCount,
                createdAt,
                recoveredAt,
                FileFailureCodes.SCAN_LEASE_EXPIRED,
                null,
                null,
                nextScanAt);
    }

    public StoredFile failPendingScan(Instant failedAt) {
        if (status != FileStatus.PENDING_SCAN) {
            throw new IllegalStateException("Only a pending scan can be failed");
        }
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                FileStatus.SCAN_FAILED,
                sizeBytes,
                sha256,
                storageKey,
                storageVersion,
                scanAttemptCount,
                createdAt,
                Objects.requireNonNull(failedAt, "failedAt must not be null"),
                FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED,
                null,
                null,
                null);
    }

    public StoredFile reject(String rejectionCode, Instant rejectedAt) {
        if (status != FileStatus.UPLOADING) {
            throw new IllegalStateException("Only an uploading file can be rejected");
        }
        return new StoredFile(
                id,
                ownerId,
                originalFilename,
                clientContentType,
                FileStatus.REJECTED,
                sizeBytes,
                sha256,
                storageKey,
                storageVersion,
                scanAttemptCount,
                createdAt,
                Objects.requireNonNull(rejectedAt, "rejectedAt must not be null"),
                requireText(rejectionCode, "rejectionCode"),
                null,
                null,
                null);
    }

    public UUID id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public String originalFilename() {
        return originalFilename;
    }

    public Optional<String> clientContentType() {
        return Optional.ofNullable(clientContentType);
    }

    public FileStatus status() {
        return status;
    }

    public Optional<Long> sizeBytes() {
        return Optional.ofNullable(sizeBytes);
    }

    public Optional<String> sha256() {
        return Optional.ofNullable(sha256);
    }

    public Optional<String> storageKey() {
        return Optional.ofNullable(storageKey);
    }

    public Optional<String> storageVersion() {
        return Optional.ofNullable(storageVersion);
    }

    public int scanAttemptCount() {
        return scanAttemptCount;
    }

    public Optional<UUID> scanLeaseId() {
        return Optional.ofNullable(scanLeaseId);
    }

    public Optional<Instant> scanLeaseUntil() {
        return Optional.ofNullable(scanLeaseUntil);
    }

    public Optional<Instant> nextScanAt() {
        return Optional.ofNullable(nextScanAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<String> failureCode() {
        return Optional.ofNullable(failureCode);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
