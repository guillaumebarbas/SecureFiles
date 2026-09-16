package com.securefiles.infrastructure.mapper;

import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.infrastructure.entity.OutboxEventEntity;
import java.util.Objects;

public final class OutboxEventMapper {

    public OutboxEventEntity toEntity(FileScanRequested scanRequest) {
        Objects.requireNonNull(scanRequest, "scanRequest must not be null");
        return new OutboxEventEntity(
                scanRequest.eventId(),
                scanRequest.fileId(),
                "FILE_SCAN_REQUESTED",
                scanRequest.sizeBytes(),
                scanRequest.sha256(),
                scanRequest.storageKey(),
                scanRequest.storageVersion(),
                scanRequest.occurredAt());
    }
}
