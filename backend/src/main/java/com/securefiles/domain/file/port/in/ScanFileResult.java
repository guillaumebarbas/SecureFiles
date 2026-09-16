package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.FileStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ScanFileResult(UUID fileId, boolean claimed, Optional<FileStatus> status) {

    public ScanFileResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (claimed && status.isEmpty()) {
            throw new IllegalArgumentException("a claimed scan must have a status");
        }
    }
}
