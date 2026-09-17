package com.securefiles.domain.file.model.list;

import java.util.Objects;

public final class ListFilesException extends RuntimeException {

    private final String code;

    public ListFilesException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public String code() {
        return code;
    }
}