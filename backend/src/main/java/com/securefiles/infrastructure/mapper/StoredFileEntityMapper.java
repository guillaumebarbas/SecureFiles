package com.securefiles.infrastructure.mapper;

import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.infrastructure.entity.QuotaAccountingState;
import com.securefiles.infrastructure.entity.StoredFileEntity;
import java.util.Objects;

public final class StoredFileEntityMapper {

    public StoredFileEntity toEntity(StoredFile storedFile) {
        Objects.requireNonNull(storedFile, "storedFile must not be null");
        return new StoredFileEntity(
                storedFile.id(),
                storedFile.ownerId(),
                storedFile.originalFilename(),
                storedFile.clientContentType().orElse(null),
                storedFile.status(),
                QuotaAccountingState.forStatus(storedFile.status()),
                storedFile.sizeBytes().orElse(null),
                storedFile.sha256().orElse(null),
                storedFile.storageKey().orElse(null),
                storedFile.storageVersion().orElse(null),
                storedFile.scanAttemptCount(),
                storedFile.scanLeaseId().orElse(null),
                storedFile.scanLeaseUntil().orElse(null),
                storedFile.nextScanAt().orElse(null),
                storedFile.failureCode().orElse(null),
                storedFile.createdAt(),
                storedFile.updatedAt());
    }

    public StoredFile toDomain(StoredFileEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return StoredFile.restore(
                entity.getId(),
                entity.getOwnerId(),
                entity.getOriginalFilename(),
                entity.getClientContentType(),
                entity.getStatus(),
                entity.getSizeBytes(),
                entity.getSha256(),
                entity.getStorageKey(),
                entity.getStorageVersion(),
                entity.getScanAttemptCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getFailureCode(),
                entity.getScanLeaseId(),
                entity.getScanLeaseUntil(),
                entity.getNextScanAt());
    }

    public void applyScanCompletion(StoredFile storedFile, StoredFileEntity entity) {
        Objects.requireNonNull(storedFile, "storedFile must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        entity.updateScanState(
                storedFile.status(),
                QuotaAccountingState.forStatus(storedFile.status()),
                storedFile.scanAttemptCount(),
                storedFile.scanLeaseId().orElse(null),
                storedFile.scanLeaseUntil().orElse(null),
                storedFile.nextScanAt().orElse(null),
                storedFile.failureCode().orElse(null),
                storedFile.updatedAt());
    }
}
