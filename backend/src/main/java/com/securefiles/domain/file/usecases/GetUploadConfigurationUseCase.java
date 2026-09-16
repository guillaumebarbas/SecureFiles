package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.port.in.GetUploadConfiguration;
import com.securefiles.domain.file.port.in.GetUploadConfigurationResult;

public final class GetUploadConfigurationUseCase implements GetUploadConfiguration {

    private final long maximumSizeBytes;

    public GetUploadConfigurationUseCase(long maximumSizeBytes) {
        this.maximumSizeBytes = maximumSizeBytes;
    }

    @Override
    public GetUploadConfigurationResult getConfiguration() {
        return new GetUploadConfigurationResult(maximumSizeBytes);
    }
}