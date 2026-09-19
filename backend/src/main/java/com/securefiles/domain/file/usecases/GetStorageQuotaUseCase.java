package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.StorageQuota;
import com.securefiles.domain.file.port.in.GetStorageQuota;
import com.securefiles.domain.file.port.in.GetStorageQuotaCommand;
import com.securefiles.domain.file.port.in.GetStorageQuotaResult;
import com.securefiles.domain.file.port.out.StorageQuotaRepository;
import java.util.Objects;

public final class GetStorageQuotaUseCase implements GetStorageQuota {

    private final StorageQuotaRepository repository;
    private final long defaultQuotaBytes;

    public GetStorageQuotaUseCase(StorageQuotaRepository repository, long defaultQuotaBytes) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        if (defaultQuotaBytes <= 0) {
            throw new IllegalArgumentException("defaultQuotaBytes must be positive");
        }
        this.defaultQuotaBytes = defaultQuotaBytes;
    }

    @Override
    public GetStorageQuotaResult get(GetStorageQuotaCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return repository.findByOwnerId(command.userId().toString())
                .map(this::toResult)
                .orElseGet(() -> new GetStorageQuotaResult(0, defaultQuotaBytes));
    }

    private GetStorageQuotaResult toResult(StorageQuota quota) {
        return new GetStorageQuotaResult(quota.usedBytes(), quota.quotaBytes());
    }
}