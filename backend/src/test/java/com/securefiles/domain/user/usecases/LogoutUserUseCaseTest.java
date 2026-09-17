package com.securefiles.domain.user.usecases;

import static org.mockito.Mockito.verify;

import com.securefiles.domain.user.port.in.LogoutUserCommand;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutUserUseCaseTest {

    private static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant LOGGED_OUT_AT = Instant.parse("2026-09-17T10:00:00Z");

    @Mock
    private AuthenticationSessionRepository sessionRepository;

    @Test
    void logout_shouldRevokeCurrentSession() {
        new LogoutUserUseCase(
                sessionRepository,
                Clock.fixed(LOGGED_OUT_AT, ZoneOffset.UTC))
                .logout(new LogoutUserCommand(SESSION_ID));

        verify(sessionRepository).revoke(SESSION_ID, LOGGED_OUT_AT);
    }
}