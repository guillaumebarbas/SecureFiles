package com.securefiles.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetStorageQuotaCommand(UUID userId) {

    public GetStorageQuotaCommand {
        Objects.requireNonNull(userId, "userId must not be null");
    }
}