package com.securefiles.infrastructure.minio;

import com.securefiles.config.UploadProperties;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class AbandonedUploadReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbandonedUploadReaper.class);

    private final StoredFileRepository repository;
    private final FileContentStorage contentStorage;
    private final Clock clock;
    private final UploadProperties properties;

    public AbandonedUploadReaper(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            Clock clock,
            UploadProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contentStorage = Objects.requireNonNull(contentStorage, "contentStorage must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Scheduled(fixedDelayString = "${securefiles.upload.recovery-interval-millis:30000}")
    public void rejectAbandonedUploads() {
        Instant now = clock.instant();
        Instant abandonedBefore = now.minus(properties.abandonedAfter());
        repository.findAbandonedUploadingIds(abandonedBefore, properties.cleanupBatchSize())
                .forEach(fileId -> deleteAbandonedUpload(fileId, now));
    }

    private void deleteAbandonedUpload(java.util.UUID fileId, Instant deletingAt) {
        try {
            if (!repository.markAbandonedUploadDeleting(
                    fileId,
                    FileFailureCodes.UPLOAD_EXPIRED,
                    deletingAt)) {
                return;
            }
            contentStorage.delete(fileId);
            repository.delete(fileId);
        } catch (RuntimeException exception) {
            LOGGER.warn("An abandoned upload could not be cleaned up", exception);
        }
    }
}