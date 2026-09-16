package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ListFilesUseCase implements ListFiles {

    private final StoredFileRepository repository;

    public ListFilesUseCase(StoredFileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public List<GetFileMetadataResult> list(ListFilesCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        List<StoredFile> storedFiles = repository.findByOwnerId(command.requesterId());
        Map<UUID, String> preciseFailureCodes = findPreciseFailureCodes(storedFiles);
        return storedFiles.stream()
                .map(storedFile -> toMetadataResult(storedFile, preciseFailureCodes))
                .toList();
    }

    private Map<UUID, String> findPreciseFailureCodes(List<StoredFile> storedFiles) {
        Set<UUID> failedFileIds = storedFiles.stream()
                .filter(this::requiresPreciseFailureCode)
            .map(storedFile -> Objects.requireNonNull(storedFile, "storedFile must not be null").id())
                .collect(Collectors.toSet());
        if (failedFileIds.isEmpty()) {
            return Map.of();
        }
        return repository.findLatestPreciseFailureCodesByFileIds(failedFileIds);
    }

    private GetFileMetadataResult toMetadataResult(
            StoredFile storedFile,
            Map<UUID, String> preciseFailureCodes) {
        return new GetFileMetadataResult(
                storedFile.id(),
                storedFile.originalFilename(),
                storedFile.sizeBytes(),
                storedFile.status(),
                storedFile.createdAt(),
                resolveFailureCode(storedFile, preciseFailureCodes));
    }

    private Optional<String> resolveFailureCode(
            StoredFile storedFile,
            Map<UUID, String> preciseFailureCodes) {
        if (!requiresPreciseFailureCode(storedFile)) {
            return storedFile.failureCode();
        }
        return Optional.ofNullable(preciseFailureCodes.get(storedFile.id()))
                .or(storedFile::failureCode);
    }

    private boolean requiresPreciseFailureCode(StoredFile storedFile) {
        return storedFile.failureCode().filter(FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED::equals).isPresent();
    }
}