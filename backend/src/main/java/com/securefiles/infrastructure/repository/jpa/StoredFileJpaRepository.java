package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.infrastructure.entity.StoredFileEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, UUID> {

    @Query(value = """
            select file.*
              from stored_file file
              left join app_user app_user
                on file.owner_id = cast(app_user.id as varchar(36))
             where file.status in (:statuses)
             order by
                case when :sort = 'NAME' and :direction = 'ASC'
                     then lower(file.original_filename) end asc nulls last,
                case when :sort = 'NAME' and :direction = 'DESC'
                     then lower(file.original_filename) end desc nulls last,
                   case when :sort = 'AUTHOR' and :direction = 'ASC'
                     then lower(coalesce(app_user.name, 'Auteur inconnu')) end asc nulls last,
                case when :sort = 'AUTHOR' and :direction = 'DESC'
                     then lower(coalesce(app_user.name, 'Auteur inconnu')) end desc nulls last,
                case when :sort = 'SIZE' and :direction = 'ASC'
                     then file.size_bytes end asc nulls last,
                case when :sort = 'SIZE' and :direction = 'DESC'
                     then file.size_bytes end desc nulls last,
                case when :sort = 'CREATED_AT' and :direction = 'ASC'
                     then file.created_at end asc nulls last,
                case when :sort = 'CREATED_AT' and :direction = 'DESC'
                     then file.created_at end desc nulls last,
                file.id desc
            """, countQuery = """
            select count(file.id)
              from stored_file file
             where file.status in (:statuses)
            """, nativeQuery = true)
    Page<StoredFileEntity> findPage(
            @Param("statuses") Collection<String> statuses,
            @Param("sort") String sort,
            @Param("direction") String direction,
            Pageable pageable);

        List<StoredFileEntity> findByOwnerIdOrderByCreatedAtDescIdDesc(String ownerId);

         @Modifying(clearAutomatically = true, flushAutomatically = true)
         @Query("""
              update StoredFileEntity file
                 set file.status = :deletingStatus,
                  file.updatedAt = :deletingAt
               where file.id = :fileId
                 and file.status in (:deletableStatuses)
              """)
         int markDeleting(
              @Param("fileId") UUID fileId,
              @Param("deletingAt") Instant deletingAt,
              @Param("deletableStatuses") Collection<FileStatus> deletableStatuses,
              @Param("deletingStatus") FileStatus deletingStatus);

         @Query("""
              select file.id
                from StoredFileEntity file
               where file.status = :deletingStatus
               order by file.updatedAt asc
              """)
         List<UUID> findDeletingIds(
              @Param("deletingStatus") FileStatus deletingStatus,
              org.springframework.data.domain.Pageable pageable);

         @Query("""
              select file.id
                from StoredFileEntity file
               where file.status = :uploadingStatus
                 and file.createdAt <= :abandonedBefore
               order by file.createdAt asc
              """)
         List<UUID> findAbandonedUploadingIds(
              @Param("uploadingStatus") FileStatus uploadingStatus,
              @Param("abandonedBefore") Instant abandonedBefore,
              org.springframework.data.domain.Pageable pageable);

         @Modifying(clearAutomatically = true, flushAutomatically = true)
         @Query("""
              update StoredFileEntity file
                 set file.status = :failedStatus,
                     file.scanLeaseId = null,
                     file.scanLeaseUntil = null,
                     file.nextScanAt = null,
                     file.failureCode = :failureCode,
                     file.updatedAt = :failedAt,
                     file.entityVersion = file.entityVersion + 1
               where file.id = :fileId
                 and file.status = :pendingStatus
              """)
         int failPendingScan(
              @Param("fileId") UUID fileId,
              @Param("failureCode") String failureCode,
              @Param("failedAt") Instant failedAt,
              @Param("pendingStatus") FileStatus pendingStatus,
              @Param("failedStatus") FileStatus failedStatus);

         @Modifying(clearAutomatically = true, flushAutomatically = true)
         @Query("""
              update StoredFileEntity file
                           set file.status = :deletingStatus,
                  file.failureCode = :failureCode,
                              file.updatedAt = :deletingAt
               where file.id = :fileId
                 and file.status = :uploadingStatus
              """)
         int markAbandonedUploadDeleting(
              @Param("fileId") UUID fileId,
              @Param("failureCode") String failureCode,
              @Param("deletingAt") Instant deletingAt,
              @Param("uploadingStatus") FileStatus uploadingStatus,
              @Param("deletingStatus") FileStatus deletingStatus);

               @Modifying(clearAutomatically = true, flushAutomatically = true)
               @Query("""
                      update StoredFileEntity file
                           set file.status = :rejectedStatus,
                              file.failureCode = :failureCode,
                              file.updatedAt = :rejectedAt
                         where file.id = :fileId
                           and file.status = :uploadingStatus
                      """)
               int rejectUpload(
                      @Param("fileId") UUID fileId,
                      @Param("failureCode") String failureCode,
                      @Param("rejectedAt") Instant rejectedAt,
                      @Param("uploadingStatus") FileStatus uploadingStatus,
                      @Param("rejectedStatus") FileStatus rejectedStatus);

      @Modifying(clearAutomatically = true, flushAutomatically = true)
      @Query("""
                    update StoredFileEntity file
                         set file.status = :pendingStatus,
                               file.sizeBytes = :sizeBytes,
                               file.sha256 = :sha256,
                               file.storageKey = :storageKey,
                               file.storageVersion = :storageVersion,
                               file.failureCode = null,
                               file.scanLeaseId = null,
                               file.scanLeaseUntil = null,
                               file.nextScanAt = null,
                               file.updatedAt = :completedAt
                     where file.id = :fileId
                         and file.status = :uploadingStatus
                    """)
      int acceptCompletedUpload(
                    @Param("fileId") UUID fileId,
                    @Param("sizeBytes") Long sizeBytes,
                    @Param("sha256") String sha256,
                    @Param("storageKey") String storageKey,
                    @Param("storageVersion") String storageVersion,
                    @Param("completedAt") Instant completedAt,
                    @Param("uploadingStatus") FileStatus uploadingStatus,
                    @Param("pendingStatus") FileStatus pendingStatus);

         @Query("""
              select file.id
                from StoredFileEntity file
               where file.status = :scanningStatus
                 and file.scanLeaseUntil is not null
                 and file.scanLeaseUntil <= :recoveredAt
               order by file.scanLeaseUntil asc
              """)
         List<UUID> findExpiredScanIds(
              @Param("recoveredAt") Instant recoveredAt,
              @Param("scanningStatus") FileStatus scanningStatus,
              org.springframework.data.domain.Pageable pageable);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
         update StoredFileEntity file
            set file.status = :completedStatus,
             file.scanLeaseId = null,
             file.scanLeaseUntil = null,
             file.nextScanAt = :nextScanAt,
             file.failureCode = :failureCode,
             file.updatedAt = :completedAt,
             file.entityVersion = file.entityVersion + 1
          where file.id = :fileId
            and file.status = :scanningStatus
            and file.scanLeaseId = :leaseId
         """)
    int completeScan(
         @Param("fileId") UUID fileId,
         @Param("leaseId") UUID leaseId,
         @Param("completedStatus") FileStatus completedStatus,
         @Param("nextScanAt") Instant nextScanAt,
         @Param("failureCode") String failureCode,
         @Param("completedAt") Instant completedAt,
         @Param("scanningStatus") FileStatus scanningStatus);

}
