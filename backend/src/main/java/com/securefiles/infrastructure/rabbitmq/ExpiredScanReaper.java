package com.securefiles.infrastructure.rabbitmq;

import com.securefiles.config.ScanProperties;
import com.securefiles.domain.file.port.in.RecoverExpiredScan;
import com.securefiles.domain.file.port.in.RecoverExpiredScanCommand;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class ExpiredScanReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredScanReaper.class);

    private final StoredFileRepository repository;
    private final RecoverExpiredScan recoverExpiredScan;
    private final Clock clock;
    private final int recoveryBatchSize;

    public ExpiredScanReaper(
            StoredFileRepository repository,
            RecoverExpiredScan recoverExpiredScan,
            Clock clock,
            ScanProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.recoverExpiredScan = Objects.requireNonNull(
                recoverExpiredScan,
                "recoverExpiredScan must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        if (properties.recoveryBatchSize() < 1) {
            throw new IllegalArgumentException("recoveryBatchSize must be positive");
        }
        this.recoveryBatchSize = properties.recoveryBatchSize();
    }

    @Scheduled(fixedDelayString = "${securefiles.scan.recovery-interval-millis:30000}")
    public void recoverExpiredLeases() {
        Instant recoveredAt = clock.instant();
        repository.findExpiredScanIds(recoveredAt, recoveryBatchSize).forEach(fileId -> {
            try {
                recoverExpiredScan.recover(new RecoverExpiredScanCommand(fileId));
            } catch (RuntimeException exception) {
                LOGGER.warn("Expired scan recovery failed for one file", exception);
            }
        });
    }
}