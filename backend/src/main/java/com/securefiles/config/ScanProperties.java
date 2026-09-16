package com.securefiles.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.scan")
public record ScanProperties(
        Duration leaseDuration,
        int maximumAttempts,
        Duration retryDelay) {
}
