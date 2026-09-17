package com.securefiles.domain.user.port.out;

import com.securefiles.domain.user.model.AuthenticationSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationSessionRepository {

    void save(AuthenticationSession session);

    Optional<AuthenticationSession> findActiveById(UUID sessionId, Instant now);

    void revoke(UUID sessionId, Instant revokedAt);
}