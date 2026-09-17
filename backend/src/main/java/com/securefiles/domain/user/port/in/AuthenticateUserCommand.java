package com.securefiles.domain.user.port.in;

import java.util.Objects;

public record AuthenticateUserCommand(String name, String password) {

    public AuthenticateUserCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }
}