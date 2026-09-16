package com.securefiles.infrastructure.rabbitmq;

import com.securefiles.domain.file.port.in.ScanFile;
import com.securefiles.domain.file.port.in.ScanFileCommand;
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

    public RabbitMqScanListener(ScanFile scanFile) {
        this.scanFile = Objects.requireNonNull(scanFile, "scanFile must not be null");
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
            if (result.status().orElse(null) == FileStatus.PENDING_SCAN) {
                channel.basicNack(deliveryTag, false, false);
            } else {
                channel.basicAck(deliveryTag, false);
            }
        } catch (RuntimeException exception) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
