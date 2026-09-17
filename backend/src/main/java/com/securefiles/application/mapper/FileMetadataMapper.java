package com.securefiles.application.mapper;

import com.securefiles.application.dto.FileMetadataResponseDto;
import com.securefiles.application.dto.ListFilesResponseDto;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.in.ListFilesResult;
import java.util.List;
import java.util.Objects;

public final class FileMetadataMapper {

    public FileMetadataResponseDto toResponse(GetFileMetadataResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new FileMetadataResponseDto(
                result.fileId(),
                result.originalFilename(),
                result.author(),
                result.sizeBytes().orElse(null),
                result.status().name(),
                result.createdAt(),
                result.failureCode().orElse(null));
    }

    public List<FileMetadataResponseDto> toResponses(List<GetFileMetadataResult> results) {
        Objects.requireNonNull(results, "results must not be null");
        return results.stream().map(this::toResponse).toList();
    }

    public ListFilesResponseDto toPageResponse(ListFilesResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new ListFilesResponseDto(
                toResponses(result.content()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious());
    }
}