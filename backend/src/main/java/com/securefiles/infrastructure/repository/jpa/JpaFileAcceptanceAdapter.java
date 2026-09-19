package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.out.FileAcceptancePort;
import com.securefiles.config.QuotaProperties;
import com.securefiles.infrastructure.entity.QuotaAccountingState;
import com.securefiles.infrastructure.mapper.OutboxEventMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaFileAcceptanceAdapter implements FileAcceptancePort {

    private final StoredFileJpaRepository storedFileRepository;
    private final OutboxEventJpaRepository outboxEventRepository;
    private final FileQuotaJpaRepository fileQuotaRepository;
    private final OutboxEventMapper outboxEventMapper;
    private final QuotaProperties quotaProperties;

    public JpaFileAcceptanceAdapter(
            StoredFileJpaRepository storedFileRepository,
            OutboxEventJpaRepository outboxEventRepository,
            FileQuotaJpaRepository fileQuotaRepository,
            OutboxEventMapper outboxEventMapper,
            QuotaProperties quotaProperties) {
        this.storedFileRepository = storedFileRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.fileQuotaRepository = fileQuotaRepository;
        this.outboxEventMapper = outboxEventMapper;
        this.quotaProperties = quotaProperties;
    }

    @Override
    @Transactional
    public boolean accept(StoredFile storedFile, FileScanRequested scanRequest) {
        long sizeBytes = storedFile.sizeBytes().orElseThrow();
        fileQuotaRepository.createIfMissing(
                storedFile.ownerId(),
                quotaProperties.perOwner().toBytes());
        if (fileQuotaRepository.reserve(storedFile.ownerId(), sizeBytes) != 1) {
            return false;
        }
        int acceptedRows = storedFileRepository.acceptCompletedUpload(
                storedFile.id(),
                sizeBytes,
                storedFile.sha256().orElseThrow(),
                storedFile.storageKey().orElseThrow(),
                storedFile.storageVersion().orElseThrow(),
                storedFile.updatedAt(),
                com.securefiles.domain.file.model.FileStatus.UPLOADING,
                storedFile.status(),
                QuotaAccountingState.RESERVED);
        if (acceptedRows != 1) {
            if (fileQuotaRepository.releaseReservation(storedFile.ownerId(), sizeBytes) != 1) {
                throw new IllegalStateException("Quota reservation could not be released");
            }
            return false;
        }
        outboxEventRepository.save(outboxEventMapper.toEntity(scanRequest));
        return true;
    }
}
