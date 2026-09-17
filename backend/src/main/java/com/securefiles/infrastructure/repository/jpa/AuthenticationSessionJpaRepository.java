package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.infrastructure.entity.AuthenticationSessionEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthenticationSessionJpaRepository
        extends JpaRepository<AuthenticationSessionEntity, UUID> {

    @Query("""
            select session from AuthenticationSessionEntity session
             where session.id = :sessionId
               and session.revokedAt is null
               and session.expiresAt > :now
            """)
    Optional<AuthenticationSessionEntity> findActiveById(
            @Param("sessionId") UUID sessionId,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthenticationSessionEntity session
               set session.revokedAt = :revokedAt
             where session.id = :sessionId
               and session.revokedAt is null
            """)
    int revoke(
            @Param("sessionId") UUID sessionId,
            @Param("revokedAt") Instant revokedAt);
}