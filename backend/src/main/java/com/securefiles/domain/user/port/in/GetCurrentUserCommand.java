package com.securefiles.domain.user.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetCurrentUserCommand(UUID userId) {

    public GetCurrentUserCommand {
        Objects.requireNonNull(userId, "userId must not be null");
    }
}