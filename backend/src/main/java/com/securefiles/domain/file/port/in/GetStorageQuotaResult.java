package com.securefiles.domain.file.port.in;

public record GetStorageQuotaResult(long usedBytes, long quotaBytes) {

    public GetStorageQuotaResult {
        if (usedBytes < 0) {
            throw new IllegalArgumentException("usedBytes must not be negative");
        }
        if (quotaBytes <= 0) {
            throw new IllegalArgumentException("quotaBytes must be positive");
        }
    }
}