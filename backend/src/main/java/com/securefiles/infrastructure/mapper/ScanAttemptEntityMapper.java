package com.securefiles.infrastructure.mapper;

import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.infrastructure.entity.ScanAttemptEntity;
import java.util.Objects;
import java.util.UUID;

public final class ScanAttemptEntityMapper {

    public ScanAttemptEntity toEntity(ScanAttempt scanAttempt) {
        Objects.requireNonNull(scanAttempt, "scanAttempt must not be null");
        return new ScanAttemptEntity(
                UUID.randomUUID(),
                scanAttempt.fileId(),
                scanAttempt.attemptNumber(),
                scanAttempt.verdict(),
                scanAttempt.failureCode(),
                scanAttempt.startedAt(),
                scanAttempt.completedAt());
    }
}
