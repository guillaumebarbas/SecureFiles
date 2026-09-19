package com.securefiles.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "securefiles.quota")
public record QuotaProperties(DataSize perOwner) {

    public QuotaProperties {
        Objects.requireNonNull(perOwner, "perOwner must not be null");
        if (perOwner.toBytes() <= 0) {
            throw new IllegalArgumentException("perOwner must be positive");
        }
    }
}