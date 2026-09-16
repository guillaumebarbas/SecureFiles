package com.securefiles.application.controller;

import com.securefiles.application.dto.UploadConfigurationResponseDto;
import com.securefiles.application.mapper.UploadConfigurationMapper;
import com.securefiles.domain.file.port.in.GetUploadConfiguration;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public final class UploadConfigurationController {

    private final GetUploadConfiguration getUploadConfiguration;
    private final UploadConfigurationMapper uploadConfigurationMapper;

    public UploadConfigurationController(
            GetUploadConfiguration getUploadConfiguration,
            UploadConfigurationMapper uploadConfigurationMapper) {
        this.getUploadConfiguration = Objects.requireNonNull(
                getUploadConfiguration,
                "getUploadConfiguration must not be null");
        this.uploadConfigurationMapper = Objects.requireNonNull(
                uploadConfigurationMapper,
                "uploadConfigurationMapper must not be null");
    }

    @GetMapping("/config")
    public UploadConfigurationResponseDto getConfiguration() {
        return uploadConfigurationMapper.toResponse(getUploadConfiguration.getConfiguration());
    }
}