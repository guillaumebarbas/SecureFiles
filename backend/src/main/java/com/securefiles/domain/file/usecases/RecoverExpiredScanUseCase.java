package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.port.in.RecoverExpiredScan;
import com.securefiles.domain.file.port.in.RecoverExpiredScanCommand;
import com.securefiles.domain.file.port.in.RecoverExpiredScanResult;
import com.securefiles.domain.file.port.out.ExpiredScanRecoveryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class RecoverExpiredScanUseCase implements RecoverExpiredScan {

    private final ExpiredScanRecoveryPort recoveryPort;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final Duration retryDelay;

    public RecoverExpiredScanUseCase(
            ExpiredScanRecoveryPort recoveryPort,
            Clock clock,
            Supplier<UUID> idGenerator,
            Duration retryDelay) {
        this.recoveryPort = Objects.requireNonNull(recoveryPort, "recoveryPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
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
        UUID eventId = Objects.requireNonNull(idGenerator.get(), "generated event id must not be null");
        return recoveryPort.recoverExpiredScan(
                        command.fileId(),
                        eventId,
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