package com.securefiles.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.rabbitmq")
public record RabbitMqProperties(
        String exchange,
        String queue,
        String routingKey,
        String retryQueue,
        String deadLetterQueue,
        int prefetch,
        long retryDelayMillis,
        long relayIntervalMillis) {
}
