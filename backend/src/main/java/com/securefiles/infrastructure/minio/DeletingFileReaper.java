package com.securefiles.infrastructure.minio;

import com.securefiles.config.StorageMaintenanceProperties;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class DeletingFileReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletingFileReaper.class);

    private final StoredFileRepository repository;
    private final FileContentStorage contentStorage;
    private final int batchSize;

    public DeletingFileReaper(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            StorageMaintenanceProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contentStorage = Objects.requireNonNull(contentStorage, "contentStorage must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.batchSize = properties.deletionRecoveryBatchSize();
    }

    @Scheduled(fixedDelayString = "${securefiles.storage.maintenance.deletion-recovery-interval-millis:30000}")
    public void retryDeletingFiles() {
        repository.findDeletingIds(batchSize).forEach(this::retryDeletion);
    }

    private void retryDeletion(java.util.UUID fileId) {
        try {
            contentStorage.delete(fileId);
            repository.delete(fileId);
        } catch (RuntimeException exception) {
            LOGGER.warn("A pending file deletion could not be completed", exception);
        }
    }
}