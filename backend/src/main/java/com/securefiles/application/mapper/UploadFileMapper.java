package com.securefiles.application.mapper;

import com.securefiles.application.dto.UploadFileRequestDto;
import com.securefiles.application.dto.UploadFileResponseDto;
import com.securefiles.domain.file.port.in.UploadFileCommand;
import com.securefiles.domain.file.port.in.UploadFileResult;
import java.util.Objects;

public final class UploadFileMapper {

    public UploadFileCommand toCommand(UploadFileRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");
        return new UploadFileCommand(
                request.ownerId(),
                request.originalFilename(),
                request.clientContentType(),
                request.declaredSizeBytes());
    }

    public UploadFileResponseDto toResponse(UploadFileResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new UploadFileResponseDto(
                result.fileId(),
                result.originalFilename(),
                result.sizeBytes(),
                result.status().name(),
                result.createdAt());
    }
}