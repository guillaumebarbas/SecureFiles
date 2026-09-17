package com.securefiles.application.controller;

import com.securefiles.application.dto.ListFilesResponseDto;
import com.securefiles.application.mapper.FileMetadataMapper;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.list.ListFilesException;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ListFilesResponseDto list(
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "10") String size) {
        return fileMetadataMapper.toPageResponse(listFiles.list(new ListFilesCommand(
                parsePaginationParameter(page),
                parsePaginationParameter(size))));
    }

    private int parsePaginationParameter(String value) {
        if (value == null) {
            throw invalidPagination();
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw invalidPagination();
        }
    }

    private ListFilesException invalidPagination() {
        return new ListFilesException(
                FileFailureCodes.INVALID_PAGINATION,
                "The page or size parameter is invalid.");
    }
}