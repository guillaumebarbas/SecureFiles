package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.infrastructure.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

        @Query("""
            select event
              from OutboxEventEntity event
             where event.publishedAt is null
               and (event.nextPublishAt is null or event.nextPublishAt <= :now)
               and (event.publishLeaseUntil is null or event.publishLeaseUntil <= :now)
             order by event.occurredAt asc
            """)
        List<OutboxEventEntity> findPendingEvents(@Param("now") Instant now, Pageable pageable);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("""
            update OutboxEventEntity event
               set event.publishLeaseId = :leaseId,
               event.publishLeaseUntil = :leaseUntil,
               event.publishAttempts = event.publishAttempts + 1
             where event.eventId = :eventId
               and event.publishedAt is null
               and (event.nextPublishAt is null or event.nextPublishAt <= :claimedAt)
               and (event.publishLeaseUntil is null or event.publishLeaseUntil <= :claimedAt)
            """)
        int claimForPublish(
            @Param("eventId") UUID eventId,
            @Param("leaseId") UUID leaseId,
            @Param("claimedAt") Instant claimedAt,
            @Param("leaseUntil") Instant leaseUntil);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("""
            update OutboxEventEntity event
               set event.publishedAt = :publishedAt,
               event.publishLeaseId = null,
               event.publishLeaseUntil = null
             where event.eventId = :eventId
               and event.publishedAt is null
               and event.publishLeaseId = :leaseId
            """)
        int markPublished(
            @Param("eventId") UUID eventId,
            @Param("leaseId") UUID leaseId,
            @Param("publishedAt") Instant publishedAt);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("""
            update OutboxEventEntity event
               set event.nextPublishAt = :nextPublishAt,
               event.publishLeaseId = null,
               event.publishLeaseUntil = null
             where event.eventId = :eventId
               and event.publishedAt is null
               and event.publishLeaseId = :leaseId
            """)
        int scheduleRetry(
            @Param("eventId") UUID eventId,
            @Param("leaseId") UUID leaseId,
            @Param("nextPublishAt") Instant nextPublishAt);

    void deleteByFileId(UUID fileId);
}
