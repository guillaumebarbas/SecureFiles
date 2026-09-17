package com.securefiles.domain.user.port.in;

import com.securefiles.domain.user.model.UserRole;
import java.util.Objects;
import java.util.Set;

public record CreateUserCommand(String name, String password, Set<UserRole> roles) {

    public CreateUserCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(password, "password must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }
}