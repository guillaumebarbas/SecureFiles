package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.delete.DeleteFileException;
import com.securefiles.domain.file.port.in.DeleteFile;
import com.securefiles.domain.file.port.in.DeleteFileCommand;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class DeleteFileUseCase implements DeleteFile {

    private final StoredFileRepository repository;
    private final FileContentStorage contentStorage;
    private final Clock clock;

    public DeleteFileUseCase(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contentStorage = Objects.requireNonNull(contentStorage, "contentStorage must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void delete(DeleteFileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StoredFile storedFile = findFile(command);
        authorizeDeletion(storedFile, command);
        ensureStatusCanBeDeleted(storedFile);
        markDeleting(storedFile, command.fileId());
        deleteContent(command);
        deleteMetadata(command);
    }

    private StoredFile findFile(DeleteFileCommand command) {
        return repository.findById(command.fileId()).orElseThrow(this::fileNotFound);
    }

    private void authorizeDeletion(StoredFile storedFile, DeleteFileCommand command) {
        if (!command.administrator() && !storedFile.ownerId().equals(command.requesterId())) {
            throw fileNotFound();
        }
    }

    private void ensureStatusCanBeDeleted(StoredFile storedFile) {
        if (storedFile.status() == FileStatus.UPLOADING || storedFile.status() == FileStatus.SCANNING) {
            throw new DeleteFileException(
                    FileFailureCodes.FILE_NOT_AVAILABLE,
                    "The file is not available for deletion.");
        }
    }

    private void deleteContent(DeleteFileCommand command) {
        try {
            contentStorage.delete(command.fileId());
        } catch (RuntimeException exception) {
            throw deletionFailed(exception);
        }
    }

    private void markDeleting(StoredFile storedFile, java.util.UUID fileId) {
        if (storedFile.status() == FileStatus.DELETING) {
            return;
        }
        try {
            if (!repository.markDeleting(fileId, Instant.now(clock))) {
                throw new DeleteFileException(
                        FileFailureCodes.FILE_NOT_AVAILABLE,
                        "The file is already being changed.");
            }
        } catch (DeleteFileException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw deletionFailed(exception);
        }
    }

    private void deleteMetadata(DeleteFileCommand command) {
        try {
            repository.delete(command.fileId());
        } catch (RuntimeException exception) {
            throw deletionFailed(exception);
        }
    }

    private DeleteFileException fileNotFound() {
        return new DeleteFileException(FileFailureCodes.FILE_NOT_FOUND, "The requested file was not found.");
    }

    private DeleteFileException deletionFailed(Throwable cause) {
        return new DeleteFileException(
                FileFailureCodes.FILE_DELETE_FAILED,
                "The file could not be deleted.",
                cause);
    }
}