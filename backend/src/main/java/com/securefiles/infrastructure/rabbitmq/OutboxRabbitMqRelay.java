package com.securefiles.infrastructure.rabbitmq;

import com.securefiles.config.RabbitMqProperties;
import com.securefiles.infrastructure.entity.OutboxEventEntity;
import com.securefiles.infrastructure.repository.jpa.OutboxEventJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class OutboxRabbitMqRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRabbitMqRelay.class);
    private static final String FILE_ID_HEADER = "securefiles-file-id";

    private final OutboxEventJpaRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;
    private final Clock clock;

    public OutboxRabbitMqRelay(
            OutboxEventJpaRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            RabbitMqProperties properties,
            Clock clock) {
        this.outboxEventRepository = Objects.requireNonNull(
                outboxEventRepository,
                "outboxEventRepository must not be null");
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelayString = "${securefiles.rabbitmq.relay-interval-millis:5000}")
    public void publishPendingEvents() {
        List<OutboxEventEntity> events = outboxEventRepository
                .findPendingEvents(Instant.now(clock), PageRequest.of(0, 100));
        for (OutboxEventEntity event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEventEntity event) {
        UUID leaseId = UUID.randomUUID();
        Instant claimedAt = Instant.now(clock);
        int claimed = outboxEventRepository.claimForPublish(
                event.getEventId(),
                leaseId,
                claimedAt,
                claimedAt.plus(properties.publishLeaseDuration()));
        if (claimed != 1) {
            return;
        }
        OutboxEventEntity claimedEvent = outboxEventRepository.findById(event.getEventId()).orElse(null);
        if (claimedEvent == null) {
            return;
        }
        RabbitMqScanMessage message = new RabbitMqScanMessage(
                claimedEvent.getEventId(),
                claimedEvent.getFileId(),
                claimedEvent.getSizeBytes(),
                claimedEvent.getSha256(),
                claimedEvent.getStorageKey(),
                claimedEvent.getStorageVersion(),
                claimedEvent.getPublishAttempts());
        try {
            CorrelationData correlationData = new CorrelationData(claimedEvent.getEventId().toString());
            rabbitTemplate.convertAndSend(
                    properties.exchange(),
                    properties.routingKey(),
                    message,
                    publishedMessage -> {
                    publishedMessage.getMessageProperties()
                        .setHeader(FILE_ID_HEADER, claimedEvent.getFileId().toString());
                    return publishedMessage;
                    },
                    correlationData);
            if (!isConfirmed(correlationData, claimedEvent)) {
                scheduleRetry(claimedEvent, leaseId);
                return;
            }
            outboxEventRepository.markPublished(
                    claimedEvent.getEventId(),
                    leaseId,
                    Instant.now(clock));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Outbox event publication failed: eventId={}, attempts={}",
                    claimedEvent.getEventId(),
                    claimedEvent.getPublishAttempts());
            scheduleRetry(claimedEvent, leaseId);
        }
    }

    private void scheduleRetry(OutboxEventEntity event, UUID leaseId) {
        int backoffExponent = Math.min(Math.max(event.getPublishAttempts() - 1, 0), 10);
        Duration retryDelay = properties.publishRetryDelay().multipliedBy(1L << backoffExponent);
        outboxEventRepository.scheduleRetry(
                event.getEventId(),
                leaseId,
                Instant.now(clock).plus(retryDelay));
    }

    private boolean isConfirmed(CorrelationData correlationData, OutboxEventEntity event) {
        try {
            CorrelationData.Confirm confirmation = correlationData.getFuture().get(
                    properties.confirmTimeoutMillis(),
                    TimeUnit.MILLISECONDS);
            if (correlationData.getReturned() != null || !confirmation.isAck()) {
                LOGGER.warn(
                        "Outbox event was not accepted by RabbitMQ: eventId={}, attempts={}",
                        event.getEventId(),
                        event.getPublishAttempts());
                return false;
            }
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Outbox event confirmation interrupted: eventId={}, attempts={}",
                    event.getEventId(),
                    event.getPublishAttempts());
            return false;
        } catch (ExecutionException | TimeoutException exception) {
            LOGGER.warn(
                    "Outbox event confirmation unavailable: eventId={}, attempts={}",
                    event.getEventId(),
                    event.getPublishAttempts());
            return false;
        }
    }
}
