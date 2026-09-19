package com.securefiles.application.controller;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.download.DownloadException;
import com.securefiles.domain.file.port.in.DownloadFile;
import com.securefiles.domain.file.port.in.DownloadFileCommand;
import com.securefiles.domain.file.port.in.DownloadFileResult;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public final class DownloadFileController {

    private final DownloadFile downloadFile;

    public DownloadFileController(DownloadFile downloadFile) {
        this.downloadFile = Objects.requireNonNull(downloadFile, "downloadFile must not be null");
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID fileId,
            Principal principal) {
        if (principal == null) {
            throw new DownloadException(FileFailureCodes.FILE_NOT_FOUND, "The requested file was not found.");
        }
        DownloadFileResult result = downloadFile.download(
                new DownloadFileCommand(fileId, principal.getName()));
        MediaType contentType = result.contentType()
                .map(this::parseContentType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(result.sizeBytes())
                .headers(headers)
                .body(new InputStreamResource(result.content()));
    }

    private MediaType parseContentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
