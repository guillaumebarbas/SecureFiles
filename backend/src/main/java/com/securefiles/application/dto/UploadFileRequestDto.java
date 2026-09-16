package com.securefiles.application.dto;

public record UploadFileRequestDto(
        String ownerId,
        String originalFilename,
        String clientContentType,
        Long declaredSizeBytes) {
}