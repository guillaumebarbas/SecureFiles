package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.list.FileListQuery;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StoredFileRepository {

    void save(StoredFile storedFile);

    Optional<StoredFile> findById(UUID fileId);

    void delete(UUID fileId);

    boolean markDeleting(UUID fileId, Instant deletingAt);

    List<UUID> findDeletingIds(int limit);

    List<UUID> findAbandonedUploadingIds(Instant abandonedBefore, int limit);

    boolean markAbandonedUploadDeleting(UUID fileId, String failureCode, Instant deletingAt);

    boolean rejectUpload(UUID fileId, String failureCode, Instant rejectedAt);

    StoredFilePage findPage(FileListQuery query);

    List<StoredFile> findByOwnerId(String ownerId);

    List<UUID> findExpiredScanIds(Instant recoveredAt, int limit);

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

    boolean completeScan(
            UUID expectedLeaseId,
            StoredFile storedFile,
            com.securefiles.domain.file.model.ScanAttempt scanAttempt);

    boolean failPendingScan(
            StoredFile storedFile,
            com.securefiles.domain.file.model.ScanAttempt scanAttempt);
}
