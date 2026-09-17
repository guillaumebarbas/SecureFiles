package com.securefiles.domain.file.model.list;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.FileStatus;
import java.util.Objects;
import java.util.Set;

public record FileListQuery(
        int page,
        int size,
        FileSortField sort,
        SortDirection direction,
        Set<FileStatus> statuses) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    public FileListQuery() {
        this(DEFAULT_PAGE, DEFAULT_SIZE, FileSortField.CREATED_AT, SortDirection.DESC, Set.of());
    }

    public FileListQuery(int page, int size) {
        this(page, size, FileSortField.CREATED_AT, SortDirection.DESC, Set.of());
    }

    public FileListQuery {
        if (page < 1 || size < 1 || size > MAX_SIZE) {
            throw invalidQuery(FileFailureCodes.INVALID_PAGINATION, "The page or size parameter is invalid.");
        }
        sort = Objects.requireNonNull(sort, "sort must not be null");
        direction = Objects.requireNonNull(direction, "direction must not be null");
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"));
    }

    private static ListFilesException invalidQuery(String code, String message) {
        return new ListFilesException(code, message);
    }
}