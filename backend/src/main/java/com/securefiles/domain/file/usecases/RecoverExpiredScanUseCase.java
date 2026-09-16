package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.port.in.RecoverExpiredScan;
import com.securefiles.domain.file.port.in.RecoverExpiredScanCommand;
import com.securefiles.domain.file.port.in.RecoverExpiredScanResult;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class RecoverExpiredScanUseCase implements RecoverExpiredScan {

    private final StoredFileRepository repository;
    private final Clock clock;
    private final Duration retryDelay;

    public RecoverExpiredScanUseCase(
            StoredFileRepository repository,
            Clock clock,
            Duration retryDelay) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(retryDelay, "retryDelay must not be null");
        if (retryDelay.isZero() || retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must be positive");
        }
        this.retryDelay = retryDelay;
    }

    @Override
    public RecoverExpiredScanResult recover(RecoverExpiredScanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant recoveredAt = clock.instant();
        return repository.recoverExpiredScan(
                        command.fileId(),
                        recoveredAt,
                        recoveredAt.plus(retryDelay))
                .map(file -> new RecoverExpiredScanResult(
                        file.id(),
                        true,
                        java.util.Optional.of(file.status())))
                .orElseGet(() -> new RecoverExpiredScanResult(
                        command.fileId(),
                        false,
                        java.util.Optional.empty()));
    }
}