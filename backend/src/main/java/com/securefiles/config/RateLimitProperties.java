package com.securefiles.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.rate-limit")
public record RateLimitProperties(
    Duration uploadWindow,
    int uploadRequestsPerWindow,
    Duration loginWindow,
    int loginRequestsPerWindow) {

    public RateLimitProperties {
        Objects.requireNonNull(uploadWindow, "uploadWindow must not be null");
        if (uploadWindow.isZero() || uploadWindow.isNegative()) {
            throw new IllegalArgumentException("uploadWindow must be positive");
        }
        if (uploadRequestsPerWindow < 1) {
            throw new IllegalArgumentException("uploadRequestsPerWindow must be positive");
        }
        Objects.requireNonNull(loginWindow, "loginWindow must not be null");
        if (loginWindow.isZero() || loginWindow.isNegative()) {
            throw new IllegalArgumentException("loginWindow must be positive");
        }
        if (loginRequestsPerWindow < 1) {
            throw new IllegalArgumentException("loginRequestsPerWindow must be positive");
        }
    }
}