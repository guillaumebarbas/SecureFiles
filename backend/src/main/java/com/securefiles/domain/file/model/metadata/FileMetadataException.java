package com.securefiles.domain.file.model.metadata;

public final class FileMetadataException extends RuntimeException {

    private final String code;

    public FileMetadataException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}