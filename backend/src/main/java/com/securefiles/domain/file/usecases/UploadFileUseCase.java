package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.StorageIntegrityException;
import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.upload.MeasuredInputStream;
import com.securefiles.domain.file.model.upload.UploadException;
import com.securefiles.domain.file.port.in.UploadFile;
import com.securefiles.domain.file.port.in.UploadFileCommand;
import com.securefiles.domain.file.port.in.UploadFileResult;
import com.securefiles.domain.file.port.out.FileAcceptancePort;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.io.InputStream;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class UploadFileUseCase implements UploadFile {

    private final StoredFileRepository repository;
    private final FileContentStorage contentStorage;
    private final FileAcceptancePort acceptancePort;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final long maximumSizeBytes;

    public UploadFileUseCase(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            FileAcceptancePort acceptancePort,
            Clock clock) {
        this(repository, contentStorage, acceptancePort, clock, UUID::randomUUID, Long.MAX_VALUE);
    }

    public UploadFileUseCase(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            FileAcceptancePort acceptancePort,
            Clock clock,
            Supplier<UUID> idGenerator) {
        this(repository, contentStorage, acceptancePort, clock, idGenerator, Long.MAX_VALUE);
    }

    public UploadFileUseCase(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            FileAcceptancePort acceptancePort,
            Clock clock,
            Supplier<UUID> idGenerator,
            long maximumSizeBytes) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contentStorage = Objects.requireNonNull(contentStorage, "contentStorage must not be null");
        this.acceptancePort = Objects.requireNonNull(acceptancePort, "acceptancePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        if (maximumSizeBytes < 0) {
            throw new IllegalArgumentException("maximumSizeBytes must not be negative");
        }
        this.maximumSizeBytes = maximumSizeBytes;
    }

    @Override
    public UploadFileResult upload(UploadFileCommand command, InputStream content) {
        validateUploadRequest(command, content);
        StoredFile uploadingFile = createUploadingFile(command);
        repository.save(uploadingFile);

        try {
            MeasuredInputStream measuredContent = new MeasuredInputStream(content, maximumSizeBytes);
            StorageReceipt storageReceipt = storeFileContent(uploadingFile, measuredContent);
            verifyStoredContent(uploadingFile, measuredContent, storageReceipt);
            StoredFile pendingScanFile = completeUploadForScanning(
                    command,
                    uploadingFile,
                    measuredContent,
                    storageReceipt);
            FileScanRequested scanRequest = createScanRequest(pendingScanFile);
            acceptCompletedUpload(pendingScanFile, scanRequest);
            return createUploadResult(pendingScanFile);
        } catch (UploadException exception) {
            rejectUploadAndCleanup(uploadingFile, exception);
            throw exception;
        } catch (StorageIntegrityException exception) {
            UploadException uploadFailure = new UploadException(
                    exception.code(),
                    "The stored file could not be verified.",
                    exception);
            rejectUploadAndCleanup(uploadingFile, uploadFailure);
            throw uploadFailure;
        } catch (RuntimeException exception) {
            UploadException uploadFailure = new UploadException(
                    FileFailureCodes.UPLOAD_FAILED,
                    "The file upload could not be accepted.",
                    exception);
            rejectUploadAndCleanup(uploadingFile, uploadFailure);
            throw uploadFailure;
        }
    }

    private void validateUploadRequest(UploadFileCommand command, InputStream content) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(content, "content must not be null");
        validateText(command.ownerId(), "ownerId");
        validateFilename(command.originalFilename());
        if (command.clientContentType() != null) {
            if (command.clientContentType().isBlank() || command.clientContentType().length() > 255) {
                throw new UploadException(
                    FileFailureCodes.INVALID_CONTENT_TYPE,
                    "The declared content type is invalid.");
            }
            if (containsControlCharacter(command.clientContentType())) {
                throw new UploadException(
                    FileFailureCodes.INVALID_CONTENT_TYPE,
                    "The declared content type is invalid.");
            }
        }
    }

    private StoredFile createUploadingFile(UploadFileCommand command) {
        UUID fileId = Objects.requireNonNull(idGenerator.get(), "generated file id must not be null");
        return StoredFile.startUpload(
                fileId,
                command.ownerId(),
                command.originalFilename(),
                command.clientContentType(),
                clock.instant());
    }

    private StorageReceipt storeFileContent(
            StoredFile uploadingFile,
            MeasuredInputStream measuredContent) {
        StorageReceipt storageReceipt = Objects.requireNonNull(
                contentStorage.store(uploadingFile.id(), measuredContent),
                "storage receipt must not be null");
        if (!measuredContent.hasReachedEnd()) {
            throw new UploadException(
                    FileFailureCodes.INCOMPLETE_STREAM,
                    "The upload stream was not fully consumed.");
        }
        return storageReceipt;
    }

    private void verifyStoredContent(
            StoredFile uploadingFile,
            MeasuredInputStream measuredContent,
            StorageReceipt storageReceipt) {
        StorageMetadata storageMetadata = Objects.requireNonNull(
                contentStorage.head(uploadingFile.id()),
                "storage metadata must not be null");
        if (storageMetadata.sizeBytes() != measuredContent.measuredSizeBytes()) {
            throw new UploadException(
                    FileFailureCodes.STORAGE_SIZE_MISMATCH,
                    "The stored file size does not match the transferred size.");
        }
        if (!storageMetadata.storageVersion().equals(storageReceipt.storageVersion())) {
            throw new UploadException(
                    FileFailureCodes.STORAGE_VERSION_MISMATCH,
                    "The stored file version does not match the storage receipt.");
        }
    }

    private StoredFile completeUploadForScanning(
            UploadFileCommand command,
            StoredFile uploadingFile,
            MeasuredInputStream measuredContent,
            StorageReceipt storageReceipt) {
        validateDeclaredSize(command, measuredContent.measuredSizeBytes());
        return uploadingFile.completeUpload(
                measuredContent.measuredSizeBytes(),
                measuredContent.sha256(),
                storageReceipt,
                clock.instant());
    }

    private void validateDeclaredSize(UploadFileCommand command, long measuredSizeBytes) {
        if (command.declaredSizeBytes() != null && command.declaredSizeBytes() != measuredSizeBytes) {
            throw new UploadException(
                    FileFailureCodes.DECLARED_SIZE_MISMATCH,
                    "The declared file size does not match the transferred size.");
        }
    }

    private FileScanRequested createScanRequest(StoredFile pendingScanFile) {
        return new FileScanRequested(
                Objects.requireNonNull(idGenerator.get(), "generated event id must not be null"),
                pendingScanFile.id(),
                pendingScanFile.sizeBytes().orElseThrow(),
                pendingScanFile.sha256().orElseThrow(),
                pendingScanFile.storageKey().orElseThrow(),
                pendingScanFile.storageVersion().orElseThrow(),
                clock.instant());
    }

    private void acceptCompletedUpload(StoredFile pendingScanFile, FileScanRequested scanRequest) {
        acceptancePort.accept(pendingScanFile, scanRequest);
    }

    private UploadFileResult createUploadResult(StoredFile pendingScanFile) {
        return new UploadFileResult(
                pendingScanFile.id(),
                pendingScanFile.originalFilename(),
                pendingScanFile.sizeBytes().orElseThrow(),
                pendingScanFile.status(),
                pendingScanFile.createdAt());
    }

    private void validateFilename(String filename) {
        validateText(filename, "originalFilename");
        if (filename.equals(".")
                || filename.equals("..")
                || filename.contains("/")
                || filename.contains("\\")
                || filename.length() > 255
                || containsControlCharacter(filename)) {
            throw new UploadException(FileFailureCodes.INVALID_FILENAME, "The filename is invalid.");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
                throw new UploadException(
                    FileFailureCodes.INVALID_FIELD_PREFIX + fieldName.toUpperCase(),
                    "The " + fieldName + " is invalid.");
        }
    }

    private boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    private void rejectUploadAndCleanup(StoredFile uploadingFile, UploadException failure) {
        try {
            contentStorage.delete(uploadingFile.id());
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
        try {
            repository.save(uploadingFile.reject(failure.code(), clock.instant()));
        } catch (RuntimeException rejectionFailure) {
            failure.addSuppressed(rejectionFailure);
        }
    }
}
