package com.securefiles.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

public record RecoverExpiredScanCommand(UUID fileId) {

    public RecoverExpiredScanCommand {
        Objects.requireNonNull(fileId, "fileId must not be null");
    }
}