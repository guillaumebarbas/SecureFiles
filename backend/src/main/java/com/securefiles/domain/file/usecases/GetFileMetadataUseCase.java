package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.metadata.FileMetadataException;
import com.securefiles.domain.file.port.in.GetFileMetadata;
import com.securefiles.domain.file.port.in.GetFileMetadataCommand;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.port.out.UserRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetFileMetadataUseCase implements GetFileMetadata {

    private final StoredFileRepository repository;
    private final UserRepository userRepository;

    public GetFileMetadataUseCase(
            StoredFileRepository repository,
            UserRepository userRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public GetFileMetadataResult get(GetFileMetadataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StoredFile storedFile = repository.findById(command.fileId())
                .orElseThrow(this::fileNotFound);
        if (!storedFile.ownerId().equals(command.requesterId())) {
            throw fileNotFound();
        }
        return new GetFileMetadataResult(
                storedFile.id(),
                storedFile.originalFilename(),
                resolveAuthor(storedFile.ownerId()),
                storedFile.sizeBytes(),
                storedFile.status(),
                storedFile.createdAt(),
                storedFile.failureCode(),
                resolveFailureCause(storedFile),
                storedFile.status() == FileStatus.CLEAN,
                isDeletable(storedFile));
    }

    private boolean isDeletable(StoredFile storedFile) {
        return storedFile.status() != FileStatus.UPLOADING && storedFile.status() != FileStatus.SCANNING;
    }

    private Optional<String> resolveFailureCause(StoredFile storedFile) {
        if (storedFile.failureCode().filter(FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED::equals).isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(repository.findLatestPreciseFailureCodesByFileIds(Set.of(storedFile.id()))
                .get(storedFile.id()))
                .filter(cause -> !FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED.equals(cause));
    }

    private String resolveAuthor(String ownerId) {
        try {
            return userRepository.findById(UUID.fromString(ownerId))
                    .map(User::name)
                    .orElse(ownerId);
        } catch (IllegalArgumentException exception) {
            return ownerId;
        }
    }

    private FileMetadataException fileNotFound() {
        return new FileMetadataException(FileFailureCodes.FILE_NOT_FOUND, "The requested file was not found.");
    }
}