package com.securefiles.domain.file.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ScanAttempt(
        UUID fileId,
        int attemptNumber,
        AntivirusVerdict verdict,
        String failureCode,
        Instant startedAt,
        Instant completedAt) {

    public ScanAttempt {
        Objects.requireNonNull(fileId, "fileId must not be null");
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber must be positive");
        }
        Objects.requireNonNull(verdict, "verdict must not be null");
        if ((verdict == AntivirusVerdict.RETRYABLE_FAILURE
                        || verdict == AntivirusVerdict.TERMINAL_FAILURE)
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("failed attempts must contain a failure code");
        }
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }
}
