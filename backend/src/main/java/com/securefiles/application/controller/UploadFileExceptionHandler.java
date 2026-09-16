package com.securefiles.application.controller;

import com.securefiles.domain.file.model.upload.UploadException;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.download.DownloadException;
import com.securefiles.domain.file.model.metadata.FileMetadataException;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public final class UploadFileExceptionHandler {

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadException(UploadException exception) {
        HttpStatus status = statusForUploadException(exception.code());
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(exception.code(), exception.getMessage()));
    }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ApiErrorResponse(
                        FileFailureCodes.MAX_SIZE_EXCEEDED,
                "The file exceeds the maximum allowed size."));
        }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> handleContentReadFailure(IOException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                    FileFailureCodes.UPLOAD_READ_FAILED,
                    "The file content could not be read."));
    }

    @ExceptionHandler(DownloadException.class)
    public ResponseEntity<ApiErrorResponse> handleDownloadException(DownloadException exception) {
        HttpStatus status = switch (exception.code()) {
            case FileFailureCodes.FILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FileFailureCodes.FILE_NOT_AVAILABLE, FileFailureCodes.STORAGE_INTEGRITY_MISMATCH -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(FileMetadataException.class)
    public ResponseEntity<ApiErrorResponse> handleFileMetadataException(FileMetadataException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(exception.code(), exception.getMessage()));
    }

    private HttpStatus statusForUploadException(String code) {
        if (code.equals(FileFailureCodes.MAX_SIZE_EXCEEDED)) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (code.startsWith(FileFailureCodes.INVALID_FIELD_PREFIX)
            || code.equals(FileFailureCodes.DECLARED_SIZE_MISMATCH)
            || code.equals(FileFailureCodes.INCOMPLETE_STREAM)) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}