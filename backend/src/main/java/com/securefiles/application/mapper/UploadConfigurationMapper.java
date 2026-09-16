package com.securefiles.application.mapper;

import com.securefiles.application.dto.UploadConfigurationResponseDto;
import com.securefiles.domain.file.port.in.GetUploadConfigurationResult;
import java.util.Objects;

public final class UploadConfigurationMapper {

    public UploadConfigurationResponseDto toResponse(GetUploadConfigurationResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new UploadConfigurationResponseDto(result.maximumSizeBytes());
    }
}