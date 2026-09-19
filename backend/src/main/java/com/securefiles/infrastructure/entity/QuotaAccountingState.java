package com.securefiles.infrastructure.entity;

import com.securefiles.domain.file.model.FileStatus;
import java.util.Objects;

public enum QuotaAccountingState {
    NONE,
    RESERVED,
    CONSUMED;

    public static QuotaAccountingState forStatus(FileStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return switch (status) {
            case PENDING_SCAN, SCANNING -> RESERVED;
            case CLEAN -> CONSUMED;
            default -> NONE;
        };
    }
}