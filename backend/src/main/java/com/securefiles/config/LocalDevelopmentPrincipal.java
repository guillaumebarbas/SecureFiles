package com.securefiles.config;

import java.security.Principal;
import java.util.Objects;

public final class LocalDevelopmentPrincipal implements Principal {

    private final String name;

    public LocalDevelopmentPrincipal(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return name;
    }
}