package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.infrastructure.entity.StoredFileQuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileQuotaJpaRepository extends JpaRepository<StoredFileQuotaEntity, String> {

    @Modifying
    @Query(value = """
            insert into stored_file_quota(owner_id, quota_bytes, used_bytes, reserved_bytes)
            values (:ownerId, :quotaBytes, 0, 0)
                                on conflict (owner_id) do update
                                        set quota_bytes = excluded.quota_bytes
            """, nativeQuery = true)
    int createIfMissing(@Param("ownerId") String ownerId, @Param("quotaBytes") long quotaBytes);

    @Modifying
    @Query(value = """
            update stored_file_quota
                                                         set reserved_bytes = reserved_bytes + :sizeBytes
             where owner_id = :ownerId
                                                         and :sizeBytes <= quota_bytes - used_bytes - reserved_bytes
            """, nativeQuery = true)
    int reserve(
            @Param("ownerId") String ownerId,
            @Param("sizeBytes") long sizeBytes);

    @Modifying
    @Query(value = """
            update stored_file_quota
               set used_bytes = used_bytes + :sizeBytes,
                   reserved_bytes = reserved_bytes - :sizeBytes
             where owner_id = :ownerId
               and reserved_bytes >= :sizeBytes
               and used_bytes <= quota_bytes - :sizeBytes
            """, nativeQuery = true)
    int consumeReservation(
            @Param("ownerId") String ownerId,
            @Param("sizeBytes") long sizeBytes);

    @Modifying
    @Query(value = """
            update stored_file_quota
               set reserved_bytes = reserved_bytes - :sizeBytes
             where owner_id = :ownerId
               and reserved_bytes >= :sizeBytes
            """, nativeQuery = true)
    int releaseReservation(
            @Param("ownerId") String ownerId,
            @Param("sizeBytes") long sizeBytes);

    @Modifying
    @Query(value = """
            update stored_file_quota
               set used_bytes = used_bytes - :sizeBytes
             where owner_id = :ownerId
               and used_bytes >= :sizeBytes
            """, nativeQuery = true)
    int releaseConsumed(
            @Param("ownerId") String ownerId,
            @Param("sizeBytes") long sizeBytes);
}