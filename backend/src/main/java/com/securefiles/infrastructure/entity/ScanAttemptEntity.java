package com.securefiles.infrastructure.entity;

import com.securefiles.domain.file.model.AntivirusVerdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scan_attempt")
public class ScanAttemptEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 32)
    private AntivirusVerdict verdict;

    @Column(name = "failure_code", length = 128)
    private String failureCode;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected ScanAttemptEntity() {
    }

    public ScanAttemptEntity(
            UUID id,
            UUID fileId,
            int attemptNumber,
            AntivirusVerdict verdict,
            String failureCode,
            Instant startedAt,
            Instant completedAt) {
        this.id = id;
        this.fileId = fileId;
        this.attemptNumber = attemptNumber;
        this.verdict = verdict;
        this.failureCode = failureCode;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AntivirusVerdict getVerdict() {
        return verdict;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
