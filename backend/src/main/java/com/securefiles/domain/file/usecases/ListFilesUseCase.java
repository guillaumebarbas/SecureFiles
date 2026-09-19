package com.securefiles.domain.file.usecases;

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
        StoredFilePage storedFilePage = repository.findPage(command.listQuery());
        List<StoredFile> storedFiles = storedFilePage.content();
        Map<String, String> authors = findAuthors(storedFiles);
        List<GetFileMetadataResult> content = storedFiles.stream()
            .map(storedFile -> toMetadataResult(storedFile, authors, command))
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

    private GetFileMetadataResult toMetadataResult(
            StoredFile storedFile,
            Map<String, String> authors,
            ListFilesCommand command) {
        return new GetFileMetadataResult(
                storedFile.id(),
                storedFile.originalFilename(),
                authors.getOrDefault(storedFile.ownerId(), UNKNOWN_AUTHOR),
                storedFile.clientContentType(),
                storedFile.sizeBytes(),
                storedFile.status(),
                storedFile.createdAt(),
                Optional.empty(),
                Optional.empty(),
                canDownload(storedFile, command),
                canDelete(storedFile, command));
    }

    private boolean canDownload(StoredFile storedFile, ListFilesCommand command) {
        return storedFile.status() == com.securefiles.domain.file.model.FileStatus.CLEAN
                && command.requesterId() != null;
    }

    private boolean canDelete(StoredFile storedFile, ListFilesCommand command) {
        boolean deletableStatus = storedFile.status() != com.securefiles.domain.file.model.FileStatus.UPLOADING
                && storedFile.status() != com.securefiles.domain.file.model.FileStatus.SCANNING
                && storedFile.status() != com.securefiles.domain.file.model.FileStatus.DELETING;
        return deletableStatus && (command.administrator() || isOwner(storedFile, command));
    }

    private boolean isOwner(StoredFile storedFile, ListFilesCommand command) {
        return command.requesterId() != null && command.requesterId().equals(storedFile.ownerId());
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

}