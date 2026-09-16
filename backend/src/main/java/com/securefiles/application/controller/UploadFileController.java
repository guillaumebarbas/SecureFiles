package com.securefiles.application.controller;

import com.securefiles.application.dto.UploadFileRequestDto;
import com.securefiles.application.dto.UploadFileResponseDto;
import com.securefiles.application.mapper.UploadFileMapper;
import com.securefiles.domain.file.port.in.UploadFile;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public final class UploadFileController {

    private final UploadFile uploadFile;
    private final UploadFileMapper uploadFileMapper;

    public UploadFileController(UploadFile uploadFile, UploadFileMapper uploadFileMapper) {
        this.uploadFile = Objects.requireNonNull(uploadFile, "uploadFile must not be null");
        this.uploadFileMapper = Objects.requireNonNull(uploadFileMapper, "uploadFileMapper must not be null");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UploadFileResponseDto upload(
            @RequestPart("file") MultipartFile file,
            Principal principal) throws IOException {
        UploadFileRequestDto request = new UploadFileRequestDto(
                principal == null ? null : principal.getName(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize());

        try (InputStream content = file.getInputStream()) {
            return uploadFileMapper.toResponse(uploadFile.upload(uploadFileMapper.toCommand(request), content));
        }
    }
}