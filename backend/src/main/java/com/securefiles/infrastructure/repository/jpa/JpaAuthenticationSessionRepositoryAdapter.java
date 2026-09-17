package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.user.model.AuthenticationSession;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import com.securefiles.infrastructure.mapper.AuthenticationSessionEntityMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaAuthenticationSessionRepositoryAdapter implements AuthenticationSessionRepository {

    private final AuthenticationSessionJpaRepository sessionRepository;
    private final AuthenticationSessionEntityMapper sessionMapper;

    public JpaAuthenticationSessionRepositoryAdapter(
            AuthenticationSessionJpaRepository sessionRepository,
            AuthenticationSessionEntityMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    @Transactional
    public void save(AuthenticationSession session) {
        sessionRepository.save(sessionMapper.toEntity(session));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticationSession> findActiveById(UUID sessionId, Instant now) {
        return sessionRepository.findActiveById(sessionId, now).map(sessionMapper::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID sessionId, Instant revokedAt) {
        sessionRepository.revoke(sessionId, revokedAt);
    }
}