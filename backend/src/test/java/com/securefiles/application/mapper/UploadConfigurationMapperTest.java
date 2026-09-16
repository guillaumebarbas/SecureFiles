package com.securefiles.application.mapper;

import com.securefiles.application.dto.UploadConfigurationResponseDto;
import com.securefiles.domain.file.port.in.GetUploadConfigurationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadConfigurationMapperTest {

    private final UploadConfigurationMapper mapper = new UploadConfigurationMapper();

    @Test
    void toResponse_shouldExposeOnlyMaximumSizeInBytes() {
        UploadConfigurationResponseDto response = mapper.toResponse(new GetUploadConfigurationResult(1024L));

        assertThat(response.maximumSizeBytes()).isEqualTo(1024L);
    }
}