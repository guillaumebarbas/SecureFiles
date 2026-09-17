package com.securefiles.domain.user.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthenticationSession {

    private final UUID id;
    private final UUID userId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Instant revokedAt;

    private AuthenticationSession(
            UUID id,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        this.revokedAt = revokedAt;
        if (revokedAt != null && revokedAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("revokedAt must not be before issuedAt");
        }
    }

    public static AuthenticationSession start(
            UUID id,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt) {
        return new AuthenticationSession(id, userId, issuedAt, expiresAt, null);
    }

    public static AuthenticationSession restore(
            UUID id,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt) {
        return new AuthenticationSession(id, userId, issuedAt, expiresAt, revokedAt);
    }

    public AuthenticationSession revoke(Instant revokedAt) {
        Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        if (this.revokedAt != null) {
            return this;
        }
        return new AuthenticationSession(id, userId, issuedAt, expiresAt, revokedAt);
    }

    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public java.util.Optional<Instant> revokedAt() {
        return java.util.Optional.ofNullable(revokedAt);
    }
}