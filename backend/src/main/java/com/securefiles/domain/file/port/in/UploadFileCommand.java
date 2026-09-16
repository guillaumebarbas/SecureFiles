package com.securefiles.domain.file.port.in;

public record UploadFileCommand(
        String ownerId,
        String originalFilename,
        String clientContentType,
        Long declaredSizeBytes) {

    public UploadFileCommand(String ownerId, String originalFilename, String clientContentType) {
        this(ownerId, originalFilename, clientContentType, null);
    }

    public UploadFileCommand {
        if (declaredSizeBytes != null && declaredSizeBytes < 0) {
            throw new IllegalArgumentException("declaredSizeBytes must not be negative");
        }
    }
}
