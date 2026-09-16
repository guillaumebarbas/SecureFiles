package com.securefiles.domain.file.model;

public class StorageIntegrityException extends RuntimeException {

    private final String code;

    public StorageIntegrityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
