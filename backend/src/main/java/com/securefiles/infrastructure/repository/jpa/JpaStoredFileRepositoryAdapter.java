package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.infrastructure.entity.StoredFileEntity;
import com.securefiles.infrastructure.mapper.ScanAttemptEntityMapper;
import com.securefiles.infrastructure.mapper.StoredFileEntityMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaStoredFileRepositoryAdapter implements StoredFileRepository {

    private final StoredFileJpaRepository storedFileRepository;
    private final ScanAttemptJpaRepository scanAttemptRepository;
    private final StoredFileEntityMapper storedFileMapper;
    private final ScanAttemptEntityMapper scanAttemptMapper;

    public JpaStoredFileRepositoryAdapter(
            StoredFileJpaRepository storedFileRepository,
            ScanAttemptJpaRepository scanAttemptRepository,
            StoredFileEntityMapper storedFileMapper,
            ScanAttemptEntityMapper scanAttemptMapper) {
        this.storedFileRepository = storedFileRepository;
        this.scanAttemptRepository = scanAttemptRepository;
        this.storedFileMapper = storedFileMapper;
        this.scanAttemptMapper = scanAttemptMapper;
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
    @Transactional(readOnly = true)
    public List<StoredFile> findAll() {
        return storedFileRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(storedFileMapper::toDomain)
                .toList();
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
    public void completeScan(StoredFile storedFile, ScanAttempt scanAttempt) {
        StoredFileEntity entity = storedFileRepository.findById(storedFile.id())
                .orElseThrow(() -> new IllegalStateException("Stored file must exist to complete scan"));
        storedFileMapper.applyScanCompletion(storedFile, entity);
        scanAttemptRepository.save(scanAttemptMapper.toEntity(scanAttempt));
    }
}
