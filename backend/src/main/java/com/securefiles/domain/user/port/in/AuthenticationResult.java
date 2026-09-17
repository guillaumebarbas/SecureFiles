package com.securefiles.domain.user.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthenticationResult(
        UserProfileResult user,
        String accessToken,
        UUID sessionId,
        Instant expiresAt) {

    public AuthenticationResult {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}