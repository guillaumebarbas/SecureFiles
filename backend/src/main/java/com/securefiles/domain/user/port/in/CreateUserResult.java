package com.securefiles.domain.user.port.in;

import com.securefiles.domain.user.model.UserRole;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CreateUserResult(UUID userId, String name, Set<UserRole> roles) {

    public CreateUserResult {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }
}