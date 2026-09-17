package com.securefiles.application.security;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public final class AuthenticatedUserPrincipal implements Principal {

    private final UUID userId;
    private final UUID sessionId;

    public AuthenticatedUserPrincipal(UUID userId, UUID sessionId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    public UUID userId() {
        return userId;
    }

    public UUID sessionId() {
        return sessionId;
    }
}