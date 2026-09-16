package com.securefiles.domain.file.port.in;

public record ListFilesCommand(String requesterId) {

    public ListFilesCommand {
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalArgumentException("requesterId must not be blank");
        }
    }
}