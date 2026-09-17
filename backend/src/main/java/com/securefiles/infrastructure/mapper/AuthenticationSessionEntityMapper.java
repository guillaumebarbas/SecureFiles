package com.securefiles.infrastructure.mapper;

import com.securefiles.domain.user.model.AuthenticationSession;
import com.securefiles.infrastructure.entity.AuthenticationSessionEntity;
import java.util.Objects;

public final class AuthenticationSessionEntityMapper {

    public AuthenticationSessionEntity toEntity(AuthenticationSession session) {
        Objects.requireNonNull(session, "session must not be null");
        return new AuthenticationSessionEntity(
                session.id(),
                session.userId(),
                session.issuedAt(),
                session.expiresAt(),
                session.revokedAt().orElse(null));
    }

    public AuthenticationSession toDomain(AuthenticationSessionEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return AuthenticationSession.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt());
    }
}