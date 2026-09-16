package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListFilesUseCaseTest {

    private static final UUID NEWEST_FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OLDEST_FILE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NEWEST_CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final Instant OLDEST_CREATED_AT = Instant.parse("2026-09-14T10:00:00Z");
    private static final String NEWEST_SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";
    private static final String OLDEST_SHA_256 = "88d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Mock
    private StoredFileRepository repository;

    private ListFilesUseCase listFilesUseCase;

    @BeforeEach
    void setUp() {
        listFilesUseCase = new ListFilesUseCase(repository);
    }

    @Test
    void list_shouldReturnMetadataInRepositoryOrder_whenRequesterOwnsFiles() {
        StoredFile newestFile = createFile(
                NEWEST_FILE_ID,
                "newest.pdf",
                42L,
                NEWEST_SHA_256,
                FileStatus.PENDING_SCAN,
                NEWEST_CREATED_AT);
        StoredFile oldestFile = createFile(
                OLDEST_FILE_ID,
                "oldest.pdf",
                12L,
                OLDEST_SHA_256,
                FileStatus.CLEAN,
                OLDEST_CREATED_AT);
        when(repository.findByOwnerId("owner-1")).thenReturn(List.of(newestFile, oldestFile));

        List<GetFileMetadataResult> result = listFilesUseCase.list(new ListFilesCommand("owner-1"));

        assertThat(result)
                .extracting(metadata -> metadata.fileId())
                .containsExactly(NEWEST_FILE_ID, OLDEST_FILE_ID);
        assertThat(result.get(0).originalFilename()).isEqualTo("newest.pdf");
        assertThat(result.get(0).sizeBytes()).contains(42L);
        assertThat(result.get(0).status()).isEqualTo(FileStatus.PENDING_SCAN);
    }

    @Test
    void list_shouldReturnEmptyMetadata_whenRequesterHasNoFiles() {
        when(repository.findByOwnerId("owner-1")).thenReturn(List.of());

        List<GetFileMetadataResult> result = listFilesUseCase.list(new ListFilesCommand("owner-1"));

        assertThat(result).isEmpty();
    }

    @Test
    void list_shouldReturnPreciseFailureCode_whenScanAttemptsAreExhausted() {
        StoredFile failedFile = createFailedFile(
                NEWEST_FILE_ID,
                "failed.pkg",
                42L,
                NEWEST_SHA_256,
                NEWEST_CREATED_AT);
        StoredFile cleanFile = createFile(
                OLDEST_FILE_ID,
                "clean.pdf",
                12L,
                OLDEST_SHA_256,
                FileStatus.CLEAN,
                OLDEST_CREATED_AT);
        when(repository.findByOwnerId("owner-1")).thenReturn(List.of(failedFile, cleanFile));
        when(repository.findLatestPreciseFailureCodesByFileIds(Set.of(NEWEST_FILE_ID)))
                .thenReturn(Map.of(NEWEST_FILE_ID, "CLAMAV_UNAVAILABLE"));

        List<GetFileMetadataResult> result = listFilesUseCase.list(new ListFilesCommand("owner-1"));

        assertThat(result.get(0).failureCode()).contains("CLAMAV_UNAVAILABLE");
        assertThat(result.get(1).failureCode()).isEmpty();
    }

    private StoredFile createFile(
            UUID fileId,
            String filename,
            long sizeBytes,
            String sha256,
            FileStatus status,
            Instant createdAt) {
        StoredFile pendingScanFile = StoredFile.startUpload(
                        fileId,
                        "owner-1",
                        filename,
                        "application/pdf",
                        createdAt)
                .completeUpload(
                        sizeBytes,
                        sha256,
                        new StorageReceipt(
                                "quarantine/" + fileId + "/payload",
                                "version-1"),
                        createdAt);

        if (status == FileStatus.PENDING_SCAN) {
            return pendingScanFile;
        }

        return pendingScanFile.claimForScan(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        createdAt.plus(5, ChronoUnit.MINUTES),
                        createdAt)
                .completeScan(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        com.securefiles.domain.file.model.AntivirusScanResult.clean(),
                        createdAt.plus(1, ChronoUnit.MINUTES));
    }

    private StoredFile createFailedFile(
            UUID fileId,
            String filename,
            long sizeBytes,
            String sha256,
            Instant createdAt) {
        UUID leaseId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        return createFile(fileId, filename, sizeBytes, sha256, FileStatus.PENDING_SCAN, createdAt)
                .claimForScan(leaseId, createdAt.plus(5, ChronoUnit.MINUTES), createdAt)
                .completeScan(
                        leaseId,
                        com.securefiles.domain.file.model.AntivirusScanResult
                                .terminalFailure("SCAN_ATTEMPTS_EXHAUSTED"),
                        createdAt.plus(1, ChronoUnit.MINUTES));
    }
}