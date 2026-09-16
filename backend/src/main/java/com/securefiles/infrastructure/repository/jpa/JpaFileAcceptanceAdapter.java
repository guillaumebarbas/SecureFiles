package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.out.FileAcceptancePort;
import com.securefiles.infrastructure.mapper.OutboxEventMapper;
import com.securefiles.infrastructure.mapper.StoredFileEntityMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaFileAcceptanceAdapter implements FileAcceptancePort {

    private final StoredFileJpaRepository storedFileRepository;
    private final OutboxEventJpaRepository outboxEventRepository;
    private final StoredFileEntityMapper storedFileMapper;
    private final OutboxEventMapper outboxEventMapper;

    public JpaFileAcceptanceAdapter(
            StoredFileJpaRepository storedFileRepository,
            OutboxEventJpaRepository outboxEventRepository,
            StoredFileEntityMapper storedFileMapper,
            OutboxEventMapper outboxEventMapper) {
        this.storedFileRepository = storedFileRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.storedFileMapper = storedFileMapper;
        this.outboxEventMapper = outboxEventMapper;
    }

    @Override
    @Transactional
    public void accept(StoredFile storedFile, FileScanRequested scanRequest) {
        storedFileRepository.save(storedFileMapper.toEntity(storedFile));
        outboxEventRepository.save(outboxEventMapper.toEntity(scanRequest));
    }
}
