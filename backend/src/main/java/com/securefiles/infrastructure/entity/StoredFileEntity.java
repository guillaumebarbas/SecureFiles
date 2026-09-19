package com.securefiles.infrastructure.entity;

import com.securefiles.domain.file.model.FileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stored_file")
public class StoredFileEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "client_content_type", length = 255)
    private String clientContentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_state", nullable = false, length = 16)
    private QuotaAccountingState quotaState;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    @Column(name = "storage_version", length = 255)
    private String storageVersion;

    @Column(name = "scan_attempt_count", nullable = false)
    private int scanAttemptCount;

    @Column(name = "scan_lease_id")
    private UUID scanLeaseId;

    @Column(name = "scan_lease_until")
    private Instant scanLeaseUntil;

    @Column(name = "next_scan_at")
    private Instant nextScanAt;

    @Column(name = "failure_code", length = 128)
    private String failureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "entity_version", nullable = false)
    private long entityVersion;

    protected StoredFileEntity() {
    }

    public StoredFileEntity(
            UUID id,
            String ownerId,
            String originalFilename,
            String clientContentType,
            FileStatus status,
            QuotaAccountingState quotaState,
            Long sizeBytes,
            String sha256,
            String storageKey,
            String storageVersion,
            int scanAttemptCount,
            UUID scanLeaseId,
            Instant scanLeaseUntil,
            Instant nextScanAt,
            String failureCode,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.originalFilename = originalFilename;
        this.clientContentType = clientContentType;
        this.status = status;
        this.quotaState = quotaState;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.storageKey = storageKey;
        this.storageVersion = storageVersion;
        this.scanAttemptCount = scanAttemptCount;
        this.scanLeaseId = scanLeaseId;
        this.scanLeaseUntil = scanLeaseUntil;
        this.nextScanAt = nextScanAt;
        this.failureCode = failureCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getClientContentType() {
        return clientContentType;
    }

    public FileStatus getStatus() {
        return status;
    }

    public QuotaAccountingState getQuotaState() {
        return quotaState;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getStorageVersion() {
        return storageVersion;
    }

    public int getScanAttemptCount() {
        return scanAttemptCount;
    }

    public UUID getScanLeaseId() {
        return scanLeaseId;
    }

    public Instant getScanLeaseUntil() {
        return scanLeaseUntil;
    }

    public Instant getNextScanAt() {
        return nextScanAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void updateScanState(
            FileStatus status,
            QuotaAccountingState quotaState,
            int scanAttemptCount,
            UUID scanLeaseId,
            Instant scanLeaseUntil,
            Instant nextScanAt,
            String failureCode,
            Instant updatedAt) {
        this.status = status;
        this.quotaState = quotaState;
        this.scanAttemptCount = scanAttemptCount;
        this.scanLeaseId = scanLeaseId;
        this.scanLeaseUntil = scanLeaseUntil;
        this.nextScanAt = nextScanAt;
        this.failureCode = failureCode;
        this.updatedAt = updatedAt;
    }
}
