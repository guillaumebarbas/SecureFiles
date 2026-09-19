package com.securefiles.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

public record FailScanCommand(UUID fileId, String failureCause) {

    public FailScanCommand {
        Objects.requireNonNull(fileId, "fileId must not be null");
        if (failureCause == null || failureCause.isBlank()) {
            throw new IllegalArgumentException("failureCause must not be blank");
        }
    }
}
