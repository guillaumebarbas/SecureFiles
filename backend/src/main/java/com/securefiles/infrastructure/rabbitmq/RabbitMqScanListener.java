package com.securefiles.infrastructure.rabbitmq;

import com.securefiles.domain.file.port.in.ScanFile;
import com.securefiles.domain.file.port.in.ScanFileCommand;
import com.securefiles.domain.file.port.in.RecoverExpiredScan;
import com.securefiles.domain.file.port.in.RecoverExpiredScanCommand;
import com.rabbitmq.client.Channel;
import com.securefiles.domain.file.model.FileStatus;
import java.io.IOException;
import java.util.Objects;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public final class RabbitMqScanListener {

    private final ScanFile scanFile;
    private final RecoverExpiredScan recoverExpiredScan;

    public RabbitMqScanListener(ScanFile scanFile, RecoverExpiredScan recoverExpiredScan) {
        this.scanFile = Objects.requireNonNull(scanFile, "scanFile must not be null");
        this.recoverExpiredScan = Objects.requireNonNull(
                recoverExpiredScan,
                "recoverExpiredScan must not be null");
    }

    @RabbitListener(
            queues = "${securefiles.rabbitmq.queue}",
            containerFactory = "scanRabbitListenerContainerFactory")
    public void handle(
            RabbitMqScanMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
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
