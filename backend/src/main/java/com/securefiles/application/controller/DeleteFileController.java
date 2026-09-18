package com.securefiles.application.controller;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.delete.DeleteFileException;
import com.securefiles.domain.file.port.in.DeleteFile;
import com.securefiles.domain.file.port.in.DeleteFileCommand;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public final class DeleteFileController {

    private final DeleteFile deleteFile;

    public DeleteFileController(DeleteFile deleteFile) {
        this.deleteFile = Objects.requireNonNull(deleteFile, "deleteFile must not be null");
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID fileId,
            Principal principal,
            Authentication authentication) {
        if (principal == null) {
            throw new DeleteFileException(FileFailureCodes.FILE_NOT_FOUND, "The requested file was not found.");
        }
        deleteFile.delete(new DeleteFileCommand(fileId, principal.getName(), isAdministrator(authentication)));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}