package com.securefiles.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

public record DeleteFileCommand(UUID fileId, String requesterId, boolean administrator) {

    public DeleteFileCommand {
        Objects.requireNonNull(fileId, "fileId must not be null");
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalArgumentException("requesterId must not be blank");
        }
    }
}