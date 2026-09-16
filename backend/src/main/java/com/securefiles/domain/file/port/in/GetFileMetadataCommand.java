package com.securefiles.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetFileMetadataCommand(UUID fileId, String requesterId) {

    public GetFileMetadataCommand {
        Objects.requireNonNull(fileId, "fileId must not be null");
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalArgumentException("requesterId must not be blank");
        }
    }
}