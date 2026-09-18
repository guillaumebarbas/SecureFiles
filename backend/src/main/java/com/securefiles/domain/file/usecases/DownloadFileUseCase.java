package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.download.DownloadException;
import com.securefiles.domain.file.port.in.DownloadFile;
import com.securefiles.domain.file.port.in.DownloadFileCommand;
import com.securefiles.domain.file.port.in.DownloadFileResult;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.io.InputStream;
import java.util.Objects;

public final class DownloadFileUseCase implements DownloadFile {

    private final StoredFileRepository repository;
    private final FileContentStorage contentStorage;

    public DownloadFileUseCase(
            StoredFileRepository repository,
            FileContentStorage contentStorage) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contentStorage = Objects.requireNonNull(contentStorage, "contentStorage must not be null");
    }

    @Override
    public DownloadFileResult download(DownloadFileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StoredFile storedFile = repository.findById(command.fileId())
                .orElseThrow(() -> fileNotFound());
        if (storedFile.status() != FileStatus.CLEAN) {
            throw new DownloadException(
                    FileFailureCodes.FILE_NOT_AVAILABLE,
                    "The file is not available for download.");
        }

        verifyStoredMetadata(storedFile);
        InputStream content;
        try {
            content = Objects.requireNonNull(
                    contentStorage.openStream(storedFile.id()),
                    "content stream must not be null");
        } catch (RuntimeException exception) {
            throw new DownloadException(
                    FileFailureCodes.CONTENT_UNAVAILABLE,
                    "The file content could not be opened.",
                    exception);
        }
        return new DownloadFileResult(
                storedFile.id(),
                storedFile.originalFilename(),
                storedFile.clientContentType(),
                storedFile.sizeBytes().orElseThrow(),
                content);
    }

    private void verifyStoredMetadata(StoredFile storedFile) {
        StorageMetadata metadata;
        try {
            metadata = contentStorage.head(storedFile.id());
        } catch (RuntimeException exception) {
            throw new DownloadException(
                    FileFailureCodes.CONTENT_UNAVAILABLE,
                    "The file content could not be verified.",
                    exception);
        }
        if (metadata == null
                || metadata.sizeBytes() != storedFile.sizeBytes().orElseThrow()
                || !metadata.storageVersion().equals(storedFile.storageVersion().orElseThrow())) {
            throw new DownloadException(
                    FileFailureCodes.STORAGE_INTEGRITY_MISMATCH,
                    "The file content failed integrity verification.");
        }
    }

    private DownloadException fileNotFound() {
        return new DownloadException(FileFailureCodes.FILE_NOT_FOUND, "The requested file was not found.");
    }
}
