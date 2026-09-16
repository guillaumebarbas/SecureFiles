package com.securefiles.domain.file.model.download;

public final class DownloadException extends RuntimeException {

    private final String code;

    public DownloadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DownloadException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
