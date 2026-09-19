package com.securefiles.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.securefiles.config.RabbitMqProperties;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.port.in.FailScan;
import com.securefiles.domain.file.port.in.FailScanCommand;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public final class RabbitMqDeadLetterListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqDeadLetterListener.class);
    private static final String FILE_ID_HEADER = "securefiles-file-id";
    private static final String REDRIVE_COUNT_HEADER = "securefiles-redrive-count";

    private final RabbitMqProperties properties;
    private final FailScan failScan;
    private final ObjectMapper objectMapper;

    public RabbitMqDeadLetterListener(
            RabbitMqProperties properties,
            FailScan failScan,
            ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.failScan = Objects.requireNonNull(failScan, "failScan must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        if (properties.maximumDeadLetterRedrives() < 1) {
            throw new IllegalArgumentException("maximumDeadLetterRedrives must be positive");
        }
    }

    @RabbitListener(
            queues = "${securefiles.rabbitmq.dead-letter-queue}",
            containerFactory = "scanRabbitListenerContainerFactory")
    public void handle(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        int currentRedriveCount = redriveCount(message);
        if (currentRedriveCount >= properties.maximumDeadLetterRedrives()) {
            try {
                completeExhaustedDeadLetter(message, channel);
                channel.basicAck(deliveryTag, false);
            } catch (IOException | RuntimeException exception) {
                try {
                    channel.basicNack(deliveryTag, false, true);
                } catch (IOException nackException) {
                    exception.addSuppressed(nackException);
                }
                if (exception instanceof IOException ioException) {
                    throw ioException;
                }
                throw (RuntimeException) exception;
            }
            return;
        }
        try {
            channel.basicPublish(
                    properties.exchange(),
                    properties.routingKey(),
                    messageProperties(message, currentRedriveCount + 1),
                    message.getBody());
            channel.basicAck(deliveryTag, false);
        } catch (IOException | RuntimeException exception) {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException nackException) {
                exception.addSuppressed(nackException);
            }
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw (RuntimeException) exception;
        }
    }

    private void completeExhaustedDeadLetter(Message message, Channel channel) throws IOException {
        Optional<UUID> fileId = extractFileId(message);
        if (fileId.isPresent()) {
            failScan.fail(new FailScanCommand(
                    fileId.get(),
                    FileFailureCodes.RABBITMQ_TRANSPORT_EXHAUSTED));
            return;
        }
        channel.basicPublish(
                properties.exchange(),
                properties.poisonQueue(),
                messageProperties(message, redriveCount(message)),
                message.getBody());
    }

    private Optional<UUID> extractFileId(Message message) {
        try {
            return Optional.of(objectMapper.readValue(message.getBody(), RabbitMqScanMessage.class).fileId());
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn(
                    "A scan message at the dead-letter limit could not be decoded ({}); using its correlation header",
                    exception.getClass().getSimpleName());
        }
        return fileIdFromHeader(message);
    }

    private Optional<UUID> fileIdFromHeader(Message message) {
        Object headerValue = message.getMessageProperties().getHeaders().get(FILE_ID_HEADER);
        if (headerValue == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(String.valueOf(headerValue)));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("A dead-letter scan message has an invalid correlation header");
            return Optional.empty();
        }
    }

    private AMQP.BasicProperties messageProperties(Message message, int redriveCount) {
        Map<String, Object> headers = sanitizedHeaders(message);
        headers.put(REDRIVE_COUNT_HEADER, redriveCount);
        return new AMQP.BasicProperties.Builder()
                .contentType(message.getMessageProperties().getContentType())
                .contentEncoding(message.getMessageProperties().getContentEncoding())
                .deliveryMode(2)
                .headers(headers)
                .build();
    }

    private Map<String, Object> sanitizedHeaders(Message message) {
        Map<String, Object> headers = new HashMap<>(message.getMessageProperties().getHeaders());
        headers.remove("x-death");
        headers.remove("x-first-death-exchange");
        headers.remove("x-first-death-queue");
        headers.remove("x-first-death-reason");
        return headers;
    }

    private int redriveCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(REDRIVE_COUNT_HEADER);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return Math.max(number.intValue(), 0);
        }
        try {
            return Math.max(Integer.parseInt(String.valueOf(value)), 0);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}