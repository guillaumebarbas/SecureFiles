package com.securefiles.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.clamav")
public record ClamAvProperties(
        String host,
        int port,
        Duration connectTimeout,
        Duration writeTimeout,
        Duration readTimeout,
        Duration scanTimeout,
        int chunkSize) {

    public ClamAvProperties {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(writeTimeout, "writeTimeout must not be null");
        Objects.requireNonNull(readTimeout, "readTimeout must not be null");
        Objects.requireNonNull(scanTimeout, "scanTimeout must not be null");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(writeTimeout, "writeTimeout");
        requirePositive(readTimeout, "readTimeout");
        requirePositive(scanTimeout, "scanTimeout");
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
    }

    private static void requirePositive(Duration value, String fieldName) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
