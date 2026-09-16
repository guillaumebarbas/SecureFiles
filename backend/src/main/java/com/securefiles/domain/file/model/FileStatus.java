package com.securefiles.domain.file.model;

public enum FileStatus {
    UPLOADING,
    PENDING_SCAN,
    SCANNING,
    CLEAN,
    INFECTED,
    SCAN_FAILED,
    REJECTED
}
