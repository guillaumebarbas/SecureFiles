package com.securefiles.application.controller;

import com.securefiles.application.dto.ListFilesResponseDto;
import com.securefiles.application.mapper.FileMetadataMapper;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.list.FileListQuery;
import com.securefiles.domain.file.model.list.ListFilesException;
import com.securefiles.domain.file.model.list.FileSortField;
import com.securefiles.domain.file.model.list.SortDirection;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(name = "status", required = false) List<String> statuses) {
        return fileMetadataMapper.toPageResponse(listFiles.list(new ListFilesCommand(new FileListQuery(
                parsePaginationParameter(page),
                parsePaginationParameter(size),
                parseSort(sort),
                parseDirection(direction),
                parseStatuses(statuses)))));
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

    private FileSortField parseSort(String value) {
        try {
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "name" -> FileSortField.NAME;
                case "author" -> FileSortField.AUTHOR;
                case "size" -> FileSortField.SIZE;
                case "createdat" -> FileSortField.CREATED_AT;
                default -> throw invalidListQuery();
            };
        } catch (NullPointerException exception) {
            throw invalidListQuery();
        }
    }

    private SortDirection parseDirection(String value) {
        try {
            return SortDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidListQuery();
        }
    }

    private Set<FileStatus> parseStatuses(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        try {
            return values.stream()
                    .flatMap(value -> Arrays.stream(value.split(",")))
                    .map(value -> FileStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidListQuery();
        }
    }

    private ListFilesException invalidListQuery() {
        return new ListFilesException(
                FileFailureCodes.INVALID_LIST_QUERY,
                "The file list query is invalid.");
    }
}