package com.securefiles.infrastructure.rabbitmq;

import com.securefiles.config.RabbitMqProperties;
import com.securefiles.infrastructure.entity.OutboxEventEntity;
import com.securefiles.infrastructure.repository.jpa.OutboxEventJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class OutboxRabbitMqRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRabbitMqRelay.class);

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
                .findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
        for (OutboxEventEntity event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEventEntity event) {
        event.incrementPublishAttempts();
        outboxEventRepository.save(event);
        RabbitMqScanMessage message = new RabbitMqScanMessage(
                event.getEventId(),
                event.getFileId(),
                event.getSizeBytes(),
                event.getSha256(),
                event.getStorageKey(),
                event.getStorageVersion(),
                event.getPublishAttempts());
        try {
            CorrelationData correlationData = new CorrelationData(event.getEventId().toString());
            rabbitTemplate.convertAndSend(
                    properties.exchange(),
                    properties.routingKey(),
                    message,
                    correlationData);
            if (!isConfirmed(correlationData, event)) {
                return;
            }
            event.markPublished(Instant.now(clock));
            outboxEventRepository.save(event);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Outbox event publication failed: eventId={}, attempts={}",
                    event.getEventId(),
                    event.getPublishAttempts());
        }
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
