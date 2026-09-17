package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import com.securefiles.domain.file.port.in.ListFilesResult;
import com.securefiles.domain.file.port.out.StoredFilePage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.domain.user.port.out.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ListFilesUseCase implements ListFiles {

    private static final String UNKNOWN_AUTHOR = "Auteur inconnu";

    private final StoredFileRepository repository;
    private final UserRepository userRepository;

    public ListFilesUseCase(StoredFileRepository repository, UserRepository userRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public ListFilesResult list(ListFilesCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StoredFilePage storedFilePage = repository.findPage(command.page(), command.size());
        List<StoredFile> storedFiles = storedFilePage.content();
        Map<String, String> authors = findAuthors(storedFiles);
        Map<UUID, String> preciseFailureCodes = findPreciseFailureCodes(storedFiles);
        List<GetFileMetadataResult> content = storedFiles.stream()
                .map(storedFile -> toMetadataResult(storedFile, authors, preciseFailureCodes))
                .toList();
        return toPageResult(command, storedFilePage, content);
    }

    private ListFilesResult toPageResult(
            ListFilesCommand command,
            StoredFilePage storedFilePage,
            List<GetFileMetadataResult> content) {
        long totalPages = storedFilePage.totalElements() == 0
                ? 0
                : ((storedFilePage.totalElements() - 1) / command.size()) + 1;
        return new ListFilesResult(
                content,
                command.page(),
                command.size(),
                storedFilePage.totalElements(),
                totalPages,
                command.page() < totalPages,
                command.page() > 1);
    }

    private Map<String, String> findAuthors(List<StoredFile> storedFiles) {
        return storedFiles.stream()
            .map(storedFile -> Objects.requireNonNull(storedFile, "storedFile must not be null").ownerId())
            .distinct()
            .collect(Collectors.toUnmodifiableMap(ownerId -> ownerId, this::resolveAuthor));
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
            Map<String, String> authors,
            Map<UUID, String> preciseFailureCodes) {
        return new GetFileMetadataResult(
                storedFile.id(),
                storedFile.originalFilename(),
                authors.getOrDefault(storedFile.ownerId(), UNKNOWN_AUTHOR),
                storedFile.sizeBytes(),
                storedFile.status(),
                storedFile.createdAt(),
                resolveFailureCode(storedFile, preciseFailureCodes));
    }

    private String resolveAuthor(String ownerId) {
        try {
            return userRepository.findById(UUID.fromString(ownerId))
                    .map(user -> Objects.requireNonNull(user, "user must not be null").name())
                    .orElse(UNKNOWN_AUTHOR);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN_AUTHOR;
        }
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