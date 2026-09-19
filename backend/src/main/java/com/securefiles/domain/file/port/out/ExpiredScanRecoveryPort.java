package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.StoredFile;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ExpiredScanRecoveryPort {

    Optional<StoredFile> recoverExpiredScan(
            UUID fileId,
            UUID eventId,
            Instant recoveredAt,
            Instant nextScanAt);
}