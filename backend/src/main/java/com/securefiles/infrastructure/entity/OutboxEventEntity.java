package com.securefiles.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "storage_version", nullable = false, length = 255)
    private String storageVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    @Column(name = "next_publish_at")
    private Instant nextPublishAt;

    @Column(name = "publish_lease_id")
    private UUID publishLeaseId;

    @Column(name = "publish_lease_until")
    private Instant publishLeaseUntil;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(
            UUID eventId,
            UUID fileId,
            String eventType,
            long sizeBytes,
            String sha256,
            String storageKey,
            String storageVersion,
            Instant occurredAt) {
        this.eventId = eventId;
        this.fileId = fileId;
        this.eventType = eventType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.storageKey = storageKey;
        this.storageVersion = storageVersion;
        this.occurredAt = occurredAt;
        this.publishAttempts = 0;
        this.nextPublishAt = occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public String getEventType() {
        return eventType;
    }

    public long getSizeBytes() {
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getPublishAttempts() {
        return publishAttempts;
    }

    public Instant getNextPublishAt() {
        return nextPublishAt;
    }

    public UUID getPublishLeaseId() {
        return publishLeaseId;
    }

    public Instant getPublishLeaseUntil() {
        return publishLeaseUntil;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void incrementPublishAttempts() {
        this.publishAttempts++;
    }
}
