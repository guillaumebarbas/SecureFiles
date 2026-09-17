package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.StoredFile;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StoredFileRepository {

    void save(StoredFile storedFile);

    Optional<StoredFile> findById(UUID fileId);

    StoredFilePage findPage(int page, int size);

    List<StoredFile> findByOwnerId(String ownerId);

    Map<UUID, String> findLatestPreciseFailureCodesByFileIds(Set<UUID> fileIds);

    Optional<StoredFile> claimPendingScan(
            UUID fileId,
            UUID leaseId,
            Instant claimedAt,
            Instant leaseUntil);

        Optional<StoredFile> recoverExpiredScan(
            UUID fileId,
            Instant recoveredAt,
            Instant nextScanAt);

    void completeScan(StoredFile storedFile, com.securefiles.domain.file.model.ScanAttempt scanAttempt);
}
