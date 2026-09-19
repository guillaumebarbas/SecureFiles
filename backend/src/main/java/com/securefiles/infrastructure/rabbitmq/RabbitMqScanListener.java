package com.securefiles.infrastructure.rabbitmq;

import com.securefiles.domain.file.port.in.ScanFile;
import com.securefiles.domain.file.port.in.ScanFileCommand;
import com.securefiles.domain.file.port.in.RecoverExpiredScan;
import com.securefiles.domain.file.port.in.RecoverExpiredScanCommand;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import com.securefiles.domain.file.model.FileStatus;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public final class RabbitMqScanListener {

    private final ScanFile scanFile;
    private final RecoverExpiredScan recoverExpiredScan;
    private final com.securefiles.config.RabbitMqProperties properties;

    public RabbitMqScanListener(
            ScanFile scanFile,
            RecoverExpiredScan recoverExpiredScan,
            com.securefiles.config.RabbitMqProperties properties) {
        this.scanFile = Objects.requireNonNull(scanFile, "scanFile must not be null");
        this.recoverExpiredScan = Objects.requireNonNull(
                recoverExpiredScan,
                "recoverExpiredScan must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        if (properties.maximumTransportRetries() < 1) {
            throw new IllegalArgumentException("maximumTransportRetries must be positive");
        }
    }

    @RabbitListener(
            queues = "${securefiles.rabbitmq.queue}",
            containerFactory = "scanRabbitListenerContainerFactory")
    public void handle(
            RabbitMqScanMessage message,
            Message rawMessage,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (transportRetriesExhausted(rawMessage)) {
            publishToDeadLetter(rawMessage, channel, deliveryTag);
            return;
        }
        try {
            var result = scanFile.scan(new ScanFileCommand(
                    message.eventId(),
                    message.fileId(),
                    message.sizeBytes(),
                    message.sha256(),
                    message.storageKey(),
                    message.storageVersion()));
            acknowledgeOrRetry(message.fileId(), result, channel, deliveryTag);
        } catch (RuntimeException exception) {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private boolean transportRetriesExhausted(Message rawMessage) {
        return transportRetryCount(rawMessage) >= properties.maximumTransportRetries();
    }

    private long transportRetryCount(Message rawMessage) {
        Object deathHeader = rawMessage.getMessageProperties().getHeaders().get("x-death");
        if (!(deathHeader instanceof List<?> deathEntries)) {
            return 0;
        }
        return deathEntries.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(this::isMainQueueRejection)
                .mapToLong(this::deathCount)
                .max()
                .orElse(0);
    }

    private boolean isMainQueueRejection(Map<?, ?> deathEntry) {
        return properties.queue().equals(String.valueOf(deathEntry.get("queue")))
                && "rejected".equals(String.valueOf(deathEntry.get("reason")));
    }

    private long deathCount(Map<?, ?> deathEntry) {
        Object count = deathEntry.get("count");
        if (count instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(count));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void publishToDeadLetter(Message rawMessage, Channel channel, long deliveryTag)
            throws IOException {
        try {
            channel.basicPublish(
                    properties.exchange(),
                    properties.deadLetterQueue(),
                    messageProperties(rawMessage),
                    rawMessage.getBody());
            channel.basicAck(deliveryTag, false);
        } catch (IOException | RuntimeException exception) {
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException nackException) {
                exception.addSuppressed(nackException);
            }
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw (RuntimeException) exception;
        }
    }

    private AMQP.BasicProperties messageProperties(Message message) {
        Map<String, Object> headers = new HashMap<>(message.getMessageProperties().getHeaders());
        headers.remove("x-death");
        headers.remove("x-first-death-exchange");
        headers.remove("x-first-death-queue");
        headers.remove("x-first-death-reason");
        return new AMQP.BasicProperties.Builder()
                .contentType(message.getMessageProperties().getContentType())
                .contentEncoding(message.getMessageProperties().getContentEncoding())
                .deliveryMode(2)
                .headers(headers)
                .build();
    }

    private void acknowledgeOrRetry(
            java.util.UUID fileId,
            com.securefiles.domain.file.port.in.ScanFileResult result,
            Channel channel,
            long deliveryTag) throws IOException {
        FileStatus status = result.status().orElse(null);
        if (status == FileStatus.PENDING_SCAN) {
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        if (status == FileStatus.SCANNING) {
            recoverExpiredScan.recover(new RecoverExpiredScanCommand(fileId));
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        channel.basicAck(deliveryTag, false);
    }
}
