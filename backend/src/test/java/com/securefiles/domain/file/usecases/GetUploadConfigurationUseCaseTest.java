package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.port.in.GetUploadConfigurationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetUploadConfigurationUseCaseTest {

    @Test
    void getConfiguration_shouldReturnConfiguredMaximumSizeInBytes() {
        var useCase = new GetUploadConfigurationUseCase(1024L);

        GetUploadConfigurationResult result = useCase.getConfiguration();

        assertThat(result.maximumSizeBytes()).isEqualTo(1024L);
    }
}