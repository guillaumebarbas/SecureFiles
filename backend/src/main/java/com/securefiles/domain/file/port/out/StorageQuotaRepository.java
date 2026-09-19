package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.StorageQuota;
import java.util.Optional;

public interface StorageQuotaRepository {

    Optional<StorageQuota> findByOwnerId(String ownerId);
}