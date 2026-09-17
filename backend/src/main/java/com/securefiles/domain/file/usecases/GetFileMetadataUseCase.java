package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileFailureCodes;
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
                resolveFailureCode(storedFile));
    }

    private Optional<String> resolveFailureCode(StoredFile storedFile) {
        Optional<String> storedFailureCode = storedFile.failureCode();
        if (storedFailureCode.filter(FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED::equals).isEmpty()) {
            return storedFailureCode;
        }
        return Optional.ofNullable(repository.findLatestPreciseFailureCodesByFileIds(Set.of(storedFile.id()))
                .get(storedFile.id()))
                .or(() -> storedFailureCode);
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