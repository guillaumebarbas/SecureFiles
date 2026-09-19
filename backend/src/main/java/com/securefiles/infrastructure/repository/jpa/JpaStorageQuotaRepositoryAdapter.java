package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.StorageQuota;
import com.securefiles.domain.file.port.out.StorageQuotaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaStorageQuotaRepositoryAdapter implements StorageQuotaRepository {

    private final FileQuotaJpaRepository quotaRepository;

    public JpaStorageQuotaRepositoryAdapter(FileQuotaJpaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StorageQuota> findByOwnerId(String ownerId) {
        return quotaRepository.findById(ownerId)
                .map(entity -> new StorageQuota(
                        entity.getOwnerId(),
                        entity.getUsedBytes(),
                        entity.getQuotaBytes()));
    }
}