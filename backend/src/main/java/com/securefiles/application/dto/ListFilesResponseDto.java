package com.securefiles.application.dto;

import java.util.List;
import java.util.Objects;

public record ListFilesResponseDto(
        List<FileMetadataResponseDto> content,
        int page,
        int size,
        long totalElements,
        long totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    public ListFilesResponseDto {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        if (totalElements < content.size()) {
            throw new IllegalArgumentException("totalElements must include content");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must not be negative");
        }
    }
}