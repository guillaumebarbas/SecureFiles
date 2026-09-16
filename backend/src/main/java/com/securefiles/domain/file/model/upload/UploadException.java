package com.securefiles.domain.file.model.upload;

public class UploadException extends RuntimeException {

    private final String code;

    public UploadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UploadException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}