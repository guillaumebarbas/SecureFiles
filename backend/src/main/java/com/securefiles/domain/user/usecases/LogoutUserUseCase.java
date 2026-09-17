package com.securefiles.domain.user.usecases;

import com.securefiles.domain.user.port.in.LogoutUser;
import com.securefiles.domain.user.port.in.LogoutUserCommand;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import java.time.Clock;
import java.util.Objects;

public final class LogoutUserUseCase implements LogoutUser {

    private final AuthenticationSessionRepository sessionRepository;
    private final Clock clock;

    public LogoutUserUseCase(AuthenticationSessionRepository sessionRepository, Clock clock) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void logout(LogoutUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        sessionRepository.revoke(command.sessionId(), clock.instant());
    }
}