package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.infrastructure.entity.StoredFileEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, UUID> {

        Page<StoredFileEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

        List<StoredFileEntity> findByOwnerIdOrderByCreatedAtDescIdDesc(String ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StoredFileEntity file
               set file.status = :scanningStatus,
                   file.scanLeaseId = :leaseId,
                   file.scanLeaseUntil = :leaseUntil,
                   file.nextScanAt = null,
                   file.failureCode = null,
                   file.scanAttemptCount = file.scanAttemptCount + 1,
                   file.updatedAt = :claimedAt
             where file.id = :fileId
               and file.status = :pendingStatus
               and (file.nextScanAt is null or file.nextScanAt <= :claimedAt)
               and (file.scanLeaseUntil is null or file.scanLeaseUntil <= :claimedAt)
            """)
    int claimPendingScan(
            @Param("fileId") UUID fileId,
            @Param("leaseId") UUID leaseId,
            @Param("claimedAt") Instant claimedAt,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("pendingStatus") FileStatus pendingStatus,
            @Param("scanningStatus") FileStatus scanningStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StoredFileEntity file
               set file.status = :pendingStatus,
                   file.scanLeaseId = null,
                   file.scanLeaseUntil = null,
                   file.nextScanAt = :nextScanAt,
                   file.failureCode = 'SCAN_LEASE_EXPIRED',
                   file.updatedAt = :recoveredAt
             where file.id = :fileId
               and file.status = :scanningStatus
               and file.scanLeaseUntil <= :recoveredAt
            """)
    int recoverExpiredScan(
            @Param("fileId") UUID fileId,
            @Param("recoveredAt") Instant recoveredAt,
            @Param("nextScanAt") Instant nextScanAt,
            @Param("pendingStatus") FileStatus pendingStatus,
            @Param("scanningStatus") FileStatus scanningStatus);
}
