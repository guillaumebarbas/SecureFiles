package com.securefiles.domain.user.model;

import java.util.Objects;

public final class UserException extends RuntimeException {

    private final String code;

    public UserException(String code, String message) {
        super(message);
        this.code = requireText(code, "code");
    }

    public String code() {
        return code;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}