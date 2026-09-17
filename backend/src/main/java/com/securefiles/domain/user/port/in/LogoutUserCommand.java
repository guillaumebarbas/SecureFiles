package com.securefiles.domain.user.port.in;

import java.util.Objects;
import java.util.UUID;

public record LogoutUserCommand(UUID sessionId) {

    public LogoutUserCommand {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
    }
}