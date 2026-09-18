package com.securefiles.domain.file.model.delete;

public final class DeleteFileException extends RuntimeException {

    private final String code;

    public DeleteFileException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DeleteFileException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}