package com.securefiles.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.clamav")
public record ClamAvProperties(
        String host,
        int port,
        Duration connectTimeout,
        Duration readTimeout,
        int chunkSize) {
}
