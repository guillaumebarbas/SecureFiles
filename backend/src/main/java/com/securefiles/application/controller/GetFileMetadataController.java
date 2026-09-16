package com.securefiles.application.controller;

import com.securefiles.application.dto.FileMetadataResponseDto;
import com.securefiles.application.mapper.FileMetadataMapper;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.metadata.FileMetadataException;
import com.securefiles.domain.file.port.in.GetFileMetadata;
import com.securefiles.domain.file.port.in.GetFileMetadataCommand;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public final class GetFileMetadataController {

    private final GetFileMetadata getFileMetadata;
    private final FileMetadataMapper fileMetadataMapper;

    public GetFileMetadataController(
            GetFileMetadata getFileMetadata,
            FileMetadataMapper fileMetadataMapper) {
        this.getFileMetadata = Objects.requireNonNull(
                getFileMetadata,
                "getFileMetadata must not be null");
        this.fileMetadataMapper = Objects.requireNonNull(
                fileMetadataMapper,
                "fileMetadataMapper must not be null");
    }

    @GetMapping("/{fileId}")
    public FileMetadataResponseDto get(
            @PathVariable UUID fileId,
            Principal principal) {
        if (principal == null) {
            throw new FileMetadataException(FileFailureCodes.FILE_NOT_FOUND, "The requested file was not found.");
        }
        return fileMetadataMapper.toResponse(getFileMetadata.get(
                new GetFileMetadataCommand(fileId, principal.getName())));
    }
}