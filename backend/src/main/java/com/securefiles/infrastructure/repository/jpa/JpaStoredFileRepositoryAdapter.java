package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.list.FileListQuery;
import com.securefiles.domain.file.port.out.ExpiredScanRecoveryPort;
import com.securefiles.domain.file.port.out.StoredFilePage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.infrastructure.entity.StoredFileEntity;
import com.securefiles.infrastructure.mapper.ScanAttemptEntityMapper;
import com.securefiles.infrastructure.mapper.OutboxEventMapper;
import com.securefiles.infrastructure.mapper.StoredFileEntityMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaStoredFileRepositoryAdapter implements StoredFileRepository, ExpiredScanRecoveryPort {

    private final StoredFileJpaRepository storedFileRepository;
    private final ScanAttemptJpaRepository scanAttemptRepository;
    private final OutboxEventJpaRepository outboxEventRepository;
    private final FileQuotaJpaRepository fileQuotaRepository;
    private final StoredFileEntityMapper storedFileMapper;
    private final ScanAttemptEntityMapper scanAttemptMapper;
    private final OutboxEventMapper outboxEventMapper;

    public JpaStoredFileRepositoryAdapter(
            StoredFileJpaRepository storedFileRepository,
            ScanAttemptJpaRepository scanAttemptRepository,
            OutboxEventJpaRepository outboxEventRepository,
            FileQuotaJpaRepository fileQuotaRepository,
            StoredFileEntityMapper storedFileMapper,
            ScanAttemptEntityMapper scanAttemptMapper,
            OutboxEventMapper outboxEventMapper) {
        this.storedFileRepository = storedFileRepository;
        this.scanAttemptRepository = scanAttemptRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.fileQuotaRepository = fileQuotaRepository;
        this.storedFileMapper = storedFileMapper;
        this.scanAttemptMapper = scanAttemptMapper;
        this.outboxEventMapper = outboxEventMapper;
    }

    @Override
    @Transactional
    public void save(StoredFile storedFile) {
        storedFileRepository.save(storedFileMapper.toEntity(storedFile));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findById(UUID fileId) {
        return storedFileRepository.findById(fileId).map(storedFileMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        StoredFileEntity storedFile = storedFileRepository.findById(fileId).orElse(null);
        releaseQuota(storedFile);
        outboxEventRepository.deleteByFileId(fileId);
        storedFileRepository.deleteById(fileId);
    }

    private void releaseQuota(StoredFileEntity storedFile) {
        if (storedFile == null
                || storedFile.getSizeBytes() == null
                || storedFile.getStatus() == FileStatus.UPLOADING
                || storedFile.getStatus() == FileStatus.REJECTED) {
            return;
        }
        if (fileQuotaRepository.release(storedFile.getOwnerId(), storedFile.getSizeBytes()) != 1) {
            throw new IllegalStateException("File quota accounting is inconsistent");
        }
    }

    @Override
    @Transactional
    public boolean markDeleting(UUID fileId, Instant deletingAt) {
        return storedFileRepository.markDeleting(
                fileId,
                deletingAt,
                List.of(
                        FileStatus.CLEAN,
                        FileStatus.INFECTED,
                        FileStatus.SCAN_FAILED,
                        FileStatus.REJECTED),
                FileStatus.DELETING) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findDeletingIds(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return storedFileRepository.findDeletingIds(FileStatus.DELETING, PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAbandonedUploadingIds(Instant abandonedBefore, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return storedFileRepository.findAbandonedUploadingIds(
                FileStatus.UPLOADING,
                abandonedBefore,
                PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public boolean markAbandonedUploadDeleting(UUID fileId, String failureCode, Instant deletingAt) {
        return storedFileRepository.markAbandonedUploadDeleting(
                fileId,
                failureCode,
                deletingAt,
                FileStatus.UPLOADING,
                FileStatus.DELETING) == 1;
    }

    @Override
    @Transactional
    public boolean rejectUpload(UUID fileId, String failureCode, Instant rejectedAt) {
        return storedFileRepository.rejectUpload(
                fileId,
                failureCode,
                rejectedAt,
                FileStatus.UPLOADING,
                FileStatus.REJECTED) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFilePage findPage(FileListQuery query) {
        Page<StoredFileEntity> storedFilePage = storedFileRepository
                .findPage(
                        statusesForQuery(query),
                        query.sort().name(),
                        query.direction().name(),
                        PageRequest.of(query.page() - 1, query.size()));
        List<StoredFile> content = storedFilePage.getContent().stream()
                .map(storedFileMapper::toDomain)
                .toList();
        return new StoredFilePage(content, storedFilePage.getTotalElements());
    }

    private Collection<String> statusesForQuery(FileListQuery query) {
        if (query.statuses().isEmpty()) {
            return java.util.Arrays.stream(FileStatus.values())
                    .filter(status -> status != FileStatus.DELETING)
                    .map(status -> status.name())
                    .toList();
        }
        return query.statuses().stream().map(status -> status.name()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFile> findByOwnerId(String ownerId) {
        return storedFileRepository.findByOwnerIdOrderByCreatedAtDescIdDesc(ownerId).stream()
                .map(storedFileMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findExpiredScanIds(Instant recoveredAt, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return storedFileRepository.findExpiredScanIds(
                recoveredAt,
                FileStatus.SCANNING,
                PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findLatestPreciseFailureCodesByFileIds(Set<UUID> fileIds) {
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return scanAttemptRepository.findLatestPreciseFailuresByFileIds(fileIds).stream()
                .collect(Collectors.toMap(
                        scanAttempt -> scanAttempt.getFileId(),
                        scanAttempt -> scanAttempt.getFailureCode()));
    }

    @Override
    @Transactional
    public Optional<StoredFile> claimPendingScan(
            UUID fileId,
            UUID leaseId,
            Instant claimedAt,
            Instant leaseUntil) {
        int updatedRows = storedFileRepository.claimPendingScan(
                fileId,
                leaseId,
                claimedAt,
                leaseUntil,
                FileStatus.PENDING_SCAN,
                FileStatus.SCANNING);
        if (updatedRows != 1) {
            return Optional.empty();
        }
        return storedFileRepository.findById(fileId).map(storedFileMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<StoredFile> recoverExpiredScan(
            UUID fileId,
            Instant recoveredAt,
            Instant nextScanAt) {
        int updatedRows = storedFileRepository.recoverExpiredScan(
                fileId,
                recoveredAt,
                nextScanAt,
                FileStatus.PENDING_SCAN,
                FileStatus.SCANNING);
        if (updatedRows != 1) {
            return Optional.empty();
        }
        return storedFileRepository.findById(fileId).map(storedFileMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<StoredFile> recoverExpiredScan(
            UUID fileId,
            UUID eventId,
            Instant recoveredAt,
            Instant nextScanAt) {
        int updatedRows = storedFileRepository.recoverExpiredScan(
                fileId,
                recoveredAt,
                nextScanAt,
                FileStatus.PENDING_SCAN,
                FileStatus.SCANNING);
        if (updatedRows != 1) {
            return Optional.empty();
        }
        StoredFile recoveredFile = storedFileRepository.findById(fileId)
                .map(storedFileMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("Recovered file could not be loaded"));
        outboxEventRepository.save(outboxEventMapper.toEntity(new FileScanRequested(
                eventId,
                recoveredFile.id(),
                recoveredFile.sizeBytes().orElseThrow(),
                recoveredFile.sha256().orElseThrow(),
                recoveredFile.storageKey().orElseThrow(),
                recoveredFile.storageVersion().orElseThrow(),
                recoveredAt)));
        return Optional.of(recoveredFile);
    }

    @Override
    @Transactional
    public boolean completeScan(UUID expectedLeaseId, StoredFile storedFile, ScanAttempt scanAttempt) {
        int updatedRows = storedFileRepository.completeScan(
                storedFile.id(),
                expectedLeaseId,
                storedFile.status(),
                storedFile.nextScanAt().orElse(null),
                storedFile.failureCode().orElse(null),
                storedFile.updatedAt(),
                FileStatus.SCANNING);
        if (updatedRows != 1) {
            return false;
        }
        scanAttemptRepository.save(scanAttemptMapper.toEntity(scanAttempt));
        return true;
    }

    @Override
    @Transactional
    public boolean failPendingScan(StoredFile storedFile, ScanAttempt scanAttempt) {
        int updatedRows = storedFileRepository.failPendingScan(
                storedFile.id(),
                storedFile.failureCode().orElseThrow(),
                storedFile.updatedAt(),
                FileStatus.PENDING_SCAN,
                FileStatus.SCAN_FAILED);
        if (updatedRows != 1) {
            return false;
        }
        scanAttemptRepository.save(scanAttemptMapper.toEntity(scanAttempt));
        return true;
    }
}
