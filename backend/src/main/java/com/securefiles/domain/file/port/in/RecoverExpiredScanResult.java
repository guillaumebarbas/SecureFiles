package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.FileStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RecoverExpiredScanResult(
        UUID fileId,
        boolean recovered,
        Optional<FileStatus> status) {

    public RecoverExpiredScanResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}