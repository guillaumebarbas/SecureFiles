package com.securefiles.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.rabbitmq")
public record RabbitMqProperties(
        String exchange,
        String queue,
        String routingKey,
        String retryQueue,
        String deadLetterQueue,
        String poisonQueue,
        int prefetch,
        long confirmTimeoutMillis,
        long relayIntervalMillis,
        int maximumTransportRetries,
        Duration publishLeaseDuration,
        Duration publishRetryDelay,
        int maximumDeadLetterRedrives) {
}
