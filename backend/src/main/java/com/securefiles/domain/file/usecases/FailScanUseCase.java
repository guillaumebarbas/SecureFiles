package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.AntivirusVerdict;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.FailScan;
import com.securefiles.domain.file.port.in.FailScanCommand;
import com.securefiles.domain.file.port.in.FailScanResult;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class FailScanUseCase implements FailScan {

    private final StoredFileRepository repository;
    private final Clock clock;

    public FailScanUseCase(StoredFileRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public FailScanResult fail(FailScanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StoredFile pendingFile = repository.findById(command.fileId()).orElse(null);
        if (pendingFile == null) {
            return new FailScanResult(command.fileId(), false, Optional.empty());
        }
        if (pendingFile.status() != FileStatus.PENDING_SCAN) {
            return new FailScanResult(
                    command.fileId(),
                    false,
                    Optional.of(pendingFile.status()));
        }

        Instant failedAt = clock.instant();
        StoredFile failedFile = pendingFile.failPendingScan(failedAt);
        ScanAttempt failureAttempt = new ScanAttempt(
                pendingFile.id(),
                Math.max(1, pendingFile.scanAttemptCount() + 1),
                AntivirusVerdict.TERMINAL_FAILURE,
                command.failureCause(),
                failedAt,
                failedAt);
        boolean failed = repository.failPendingScan(failedFile, failureAttempt);
        return new FailScanResult(
                command.fileId(),
                failed,
            Optional.of(failedFile.status()));
    }
}
