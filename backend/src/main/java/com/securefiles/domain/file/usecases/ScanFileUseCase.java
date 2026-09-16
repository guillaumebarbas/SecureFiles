package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.AntivirusScanResult;
import com.securefiles.domain.file.model.AntivirusVerdict;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.domain.file.model.StorageIntegrityException;
import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.upload.MeasuredInputStream;
import com.securefiles.domain.file.port.in.ScanFile;
import com.securefiles.domain.file.port.in.ScanFileCommand;
import com.securefiles.domain.file.port.in.ScanFileResult;
import com.securefiles.domain.file.port.out.AntivirusScanner;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ScanFileUseCase implements ScanFile {

    private final StoredFileRepository repository;
    private final FileContentStorage contentStorage;
    private final AntivirusScanner antivirusScanner;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final Duration leaseDuration;
    private final int maximumAttempts;
    private final Duration retryDelay;

    public ScanFileUseCase(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            AntivirusScanner antivirusScanner,
            Clock clock,
            Supplier<UUID> idGenerator,
            Duration leaseDuration,
            int maximumAttempts,
            Duration retryDelay) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contentStorage = Objects.requireNonNull(contentStorage, "contentStorage must not be null");
        this.antivirusScanner = Objects.requireNonNull(antivirusScanner, "antivirusScanner must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.leaseDuration = requirePositive(leaseDuration, "leaseDuration");
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("maximumAttempts must be positive");
        }
        this.maximumAttempts = maximumAttempts;
        this.retryDelay = requirePositive(retryDelay, "retryDelay");
    }

    @Override
    public ScanFileResult scan(ScanFileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant scanStartedAt = clock.instant();
        UUID leaseId = Objects.requireNonNull(idGenerator.get(), "generated lease id must not be null");
        Instant leaseUntil = scanStartedAt.plus(leaseDuration);
        Optional<StoredFile> claimedFile = repository.claimPendingScan(
                command.fileId(),
                leaseId,
                scanStartedAt,
                leaseUntil);
        if (claimedFile.isEmpty()) {
            return new ScanFileResult(command.fileId(), false, Optional.empty());
        }

        StoredFile scanningFile = claimedFile.orElseThrow();
        AntivirusScanResult scanResult = scanContent(command, scanningFile);
        AntivirusScanResult completionResult = normalizeFailureAfterAttemptLimit(scanResult, scanningFile);
        Instant completedAt = clock.instant();
        StoredFile completedFile = scanningFile.completeScan(
                leaseId,
            completionResult,
                completedAt,
                completedAt.plus(retryDelay));
        repository.completeScan(
                completedFile,
                new ScanAttempt(
                        completedFile.id(),
                        completedFile.scanAttemptCount(),
                        scanResult.verdict(),
                        scanResult.failureCode(),
                        scanStartedAt,
                        completedAt));
        return new ScanFileResult(completedFile.id(), true, Optional.of(completedFile.status()));
    }

    private AntivirusScanResult scanContent(ScanFileCommand command, StoredFile scanningFile) {
        if (!messageMetadataMatches(command, scanningFile)) {
            return AntivirusScanResult.terminalFailure(FileFailureCodes.SCAN_MESSAGE_MISMATCH);
        }
        try {
            verifyStoredMetadata(scanningFile, contentStorage.head(scanningFile.id()));
            try (InputStream content = contentStorage.openStream(scanningFile.id())) {
                MeasuredInputStream measuredContent = new MeasuredInputStream(content);
                AntivirusScanResult scanResult = Objects.requireNonNull(
                        antivirusScanner.scan(measuredContent),
                        "antivirus scan result must not be null");
                verifyMeasuredContent(scanningFile, measuredContent);
                return scanResult;
            }
        } catch (StorageIntegrityException exception) {
            return AntivirusScanResult.terminalFailure(exception.code());
        } catch (IOException | RuntimeException exception) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.ANTIVIRUS_UNAVAILABLE);
        }
    }

    private boolean messageMetadataMatches(ScanFileCommand command, StoredFile scanningFile) {
        if (command.expectedSizeBytes() == null) {
            return true;
        }
        return command.expectedSizeBytes().equals(scanningFile.sizeBytes().orElse(null))
                && command.expectedSha256().equals(scanningFile.sha256().orElse(null))
                && command.expectedStorageKey().equals(scanningFile.storageKey().orElse(null))
                && command.expectedStorageVersion().equals(scanningFile.storageVersion().orElse(null));
    }

    private void verifyStoredMetadata(StoredFile scanningFile, StorageMetadata storageMetadata) {
        Objects.requireNonNull(storageMetadata, "storage metadata must not be null");
        long expectedSize = scanningFile.sizeBytes().orElseThrow();
        String expectedVersion = scanningFile.storageVersion().orElseThrow();
        if (storageMetadata.sizeBytes() != expectedSize) {
            throw new StorageIntegrityException(
                FileFailureCodes.STORAGE_SIZE_MISMATCH,
                "Stored object size does not match the file metadata");
        }
        if (!storageMetadata.storageVersion().equals(expectedVersion)) {
            throw new StorageIntegrityException(
                FileFailureCodes.STORAGE_VERSION_MISMATCH,
                "Stored object version does not match the file metadata");
        }
    }

    private void verifyMeasuredContent(StoredFile scanningFile, MeasuredInputStream measuredContent) {
        if (!measuredContent.hasReachedEnd()) {
            throw new IllegalStateException("Antivirus scanner did not consume the complete content");
        }
        long expectedSize = scanningFile.sizeBytes().orElseThrow();
        String expectedSha256 = scanningFile.sha256().orElseThrow();
        if (measuredContent.measuredSizeBytes() != expectedSize) {
            throw new StorageIntegrityException(
                FileFailureCodes.STORAGE_CONTENT_SIZE_MISMATCH,
                "Scanned content size does not match the file metadata");
        }
        if (!measuredContent.sha256().equals(expectedSha256)) {
            throw new StorageIntegrityException(
                FileFailureCodes.STORAGE_CONTENT_SHA256_MISMATCH,
                "Scanned content hash does not match the file metadata");
        }
    }

    private AntivirusScanResult normalizeFailureAfterAttemptLimit(
            AntivirusScanResult scanResult,
            StoredFile scanningFile) {
        if (scanResult.verdict() == AntivirusVerdict.RETRYABLE_FAILURE
                && scanningFile.scanAttemptCount() >= maximumAttempts) {
            return AntivirusScanResult.terminalFailure(FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED);
        }
        return scanResult;
    }

    private Duration requirePositive(Duration value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
