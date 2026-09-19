package com.securefiles.application.controller;

import com.securefiles.application.dto.StorageQuotaResponseDto;
import com.securefiles.application.mapper.StorageQuotaMapper;
import com.securefiles.application.security.AuthenticatedUserPrincipal;
import com.securefiles.domain.file.port.in.GetStorageQuota;
import com.securefiles.domain.file.port.in.GetStorageQuotaCommand;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public final class StorageQuotaController {

    private final GetStorageQuota getStorageQuota;
    private final StorageQuotaMapper storageQuotaMapper;

    public StorageQuotaController(GetStorageQuota getStorageQuota, StorageQuotaMapper storageQuotaMapper) {
        this.getStorageQuota = Objects.requireNonNull(getStorageQuota, "getStorageQuota must not be null");
        this.storageQuotaMapper = Objects.requireNonNull(
                storageQuotaMapper,
                "storageQuotaMapper must not be null");
    }

    @GetMapping("/storage")
    public ResponseEntity<StorageQuotaResponseDto> get(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.noContent().build();
        }
        GetStorageQuotaCommand command = new GetStorageQuotaCommand(principal.userId());
        return ResponseEntity.ok(storageQuotaMapper.toResponse(getStorageQuota.get(command)));
    }
}