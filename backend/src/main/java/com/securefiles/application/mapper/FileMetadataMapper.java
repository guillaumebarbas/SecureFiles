package com.securefiles.application.mapper;

import com.securefiles.application.dto.FileMetadataResponseDto;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import java.util.List;
import java.util.Objects;

public final class FileMetadataMapper {

    public FileMetadataResponseDto toResponse(GetFileMetadataResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new FileMetadataResponseDto(
                result.fileId(),
                result.originalFilename(),
                result.sizeBytes().orElse(null),
                result.status().name(),
                result.createdAt(),
                result.failureCode().orElse(null));
    }

    public List<FileMetadataResponseDto> toResponses(List<GetFileMetadataResult> results) {
        Objects.requireNonNull(results, "results must not be null");
        return results.stream().map(this::toResponse).toList();
    }
}