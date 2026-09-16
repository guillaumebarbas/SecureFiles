package com.securefiles.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.local.identity")
public record LocalDevelopmentIdentityProperties(String ownerId) {

    public LocalDevelopmentIdentityProperties {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        Objects.requireNonNull(ownerId, "ownerId must not be null");
    }
}