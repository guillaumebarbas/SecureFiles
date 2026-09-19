package com.securefiles.application.mapper;

import com.securefiles.application.dto.StorageQuotaResponseDto;
import com.securefiles.domain.file.port.in.GetStorageQuotaResult;
import java.util.Objects;

public final class StorageQuotaMapper {

    public StorageQuotaResponseDto toResponse(GetStorageQuotaResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new StorageQuotaResponseDto(result.usedBytes(), result.quotaBytes());
    }
}