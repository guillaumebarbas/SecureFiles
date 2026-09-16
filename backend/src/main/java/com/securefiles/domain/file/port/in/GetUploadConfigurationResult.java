package com.securefiles.domain.file.port.in;

public record GetUploadConfigurationResult(long maximumSizeBytes) {

    public GetUploadConfigurationResult {
        if (maximumSizeBytes <= 0) {
            throw new IllegalArgumentException("maximumSizeBytes must be positive");
        }
    }
}