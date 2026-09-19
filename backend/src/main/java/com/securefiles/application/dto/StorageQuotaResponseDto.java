package com.securefiles.application.dto;

public record StorageQuotaResponseDto(long usedBytes, long quotaBytes) {
}