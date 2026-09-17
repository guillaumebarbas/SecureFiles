package com.securefiles.application.controller;

import com.securefiles.application.dto.FileMetadataResponseDto;
import com.securefiles.application.mapper.FileMetadataMapper;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public final class ListFilesController {

    private final ListFiles listFiles;
    private final FileMetadataMapper fileMetadataMapper;

    public ListFilesController(ListFiles listFiles, FileMetadataMapper fileMetadataMapper) {
        this.listFiles = Objects.requireNonNull(listFiles, "listFiles must not be null");
        this.fileMetadataMapper = Objects.requireNonNull(
                fileMetadataMapper,
                "fileMetadataMapper must not be null");
    }

    @GetMapping
    public List<FileMetadataResponseDto> list() {
        return fileMetadataMapper.toResponses(listFiles.list(new ListFilesCommand()));
    }
}