package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.AntivirusScanResult;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.domain.file.model.StorageObjectNotFoundException;
import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.ScanFileCommand;
import com.securefiles.domain.file.port.in.ScanFileResult;
import com.securefiles.domain.file.port.out.AntivirusScanner;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanFileUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LEASE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant STARTED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final String SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Mock
    private StoredFileRepository repository;

    @Mock
    private FileContentStorage contentStorage;

    @Mock
    private AntivirusScanner antivirusScanner;

    private ScanFileUseCase scanFileUseCase;

    @BeforeEach
    void setUp() {
        scanFileUseCase = new ScanFileUseCase(
                repository,
                contentStorage,
                antivirusScanner,
                Clock.fixed(STARTED_AT, ZoneOffset.UTC),
                () -> LEASE_ID,
                Duration.ofSeconds(30),
                3,
                Duration.ofSeconds(60));
    }

    @Test
    void scan_shouldMarkFileClean_whenAntivirusReturnsClean() throws Exception {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
                LEASE_ID,
                STARTED_AT.plusSeconds(30),
                STARTED_AT);
        when(repository.claimPendingScan(
                eq(FILE_ID),
                eq(LEASE_ID),
                eq(STARTED_AT),
                eq(STARTED_AT.plusSeconds(30))))
                .thenReturn(Optional.of(scanningFile));
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));
        when(contentStorage.openStream(FILE_ID)).thenReturn(
                new ByteArrayInputStream("safe content".getBytes()));
        when(antivirusScanner.scan(any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(0);
            content.transferTo(OutputStream.nullOutputStream());
            return AntivirusScanResult.clean();
        });

        ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(FILE_ID));

        assertThat(result.claimed()).isTrue();
        assertThat(result.status()).contains(FileStatus.CLEAN);
        ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).completeScan(storedFile.capture(), any());
        assertThat(storedFile.getValue().status()).isEqualTo(FileStatus.CLEAN);
    }

    @Test
    void scan_shouldReturnUnclaimed_whenRepositoryCannotClaimFile() {
        when(repository.claimPendingScan(
                eq(FILE_ID),
                eq(LEASE_ID),
                eq(STARTED_AT),
                eq(STARTED_AT.plusSeconds(30))))
                .thenReturn(Optional.empty());

        ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(FILE_ID));

        assertThat(result.claimed()).isFalse();
        assertThat(result.status()).isEmpty();
        verifyNoInteractions(contentStorage, antivirusScanner);
        verify(repository, never()).completeScan(any(), any());
    }

    @Test
    void scan_shouldReturnToPendingScan_whenAntivirusFailsBeforeAttemptsAreExhausted() throws Exception {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
                LEASE_ID,
                STARTED_AT.plusSeconds(30),
                STARTED_AT);
        when(repository.claimPendingScan(
                eq(FILE_ID),
                eq(LEASE_ID),
                eq(STARTED_AT),
                eq(STARTED_AT.plusSeconds(30))))
                .thenReturn(Optional.of(scanningFile));
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));
        when(contentStorage.openStream(FILE_ID)).thenReturn(
                new ByteArrayInputStream("safe content".getBytes()));
        when(antivirusScanner.scan(any(InputStream.class)))
                .thenThrow(new IllegalStateException("clamav unavailable"));

        ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(FILE_ID));

        assertThat(result.status()).contains(FileStatus.PENDING_SCAN);
        ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).completeScan(storedFile.capture(), any());
        assertThat(storedFile.getValue().failureCode()).contains("ANTIVIRUS_UNAVAILABLE");
    }

    @Test
    void scan_shouldFailClosedWithObjectNotFoundCode_whenStoredObjectIsMissing() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
                LEASE_ID,
                STARTED_AT.plusSeconds(30),
                STARTED_AT);
        when(repository.claimPendingScan(
                eq(FILE_ID),
                eq(LEASE_ID),
                eq(STARTED_AT),
                eq(STARTED_AT.plusSeconds(30))))
                .thenReturn(Optional.of(scanningFile));
        when(contentStorage.head(FILE_ID)).thenThrow(new StorageObjectNotFoundException());

        ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(FILE_ID));

        assertThat(result.status()).contains(FileStatus.SCAN_FAILED);
        ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).completeScan(storedFile.capture(), any());
        assertThat(storedFile.getValue().failureCode()).contains("STORAGE_OBJECT_NOT_FOUND");
    }

    @Test
    void scan_shouldFailClosedWithSizeMismatchCode_whenStoredObjectSizeDiffers() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
                LEASE_ID,
                STARTED_AT.plusSeconds(30),
                STARTED_AT);
        when(repository.claimPendingScan(
                eq(FILE_ID),
                eq(LEASE_ID),
                eq(STARTED_AT),
                eq(STARTED_AT.plusSeconds(30))))
                .thenReturn(Optional.of(scanningFile));
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(13L, "version-1"));

        ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(FILE_ID));

        assertThat(result.status()).contains(FileStatus.SCAN_FAILED);
        ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).completeScan(storedFile.capture(), any());
        assertThat(storedFile.getValue().failureCode()).contains("STORAGE_SIZE_MISMATCH");
    }

        @Test
        void scan_shouldPreserveOriginalFailureCodeInAttempt_whenAttemptsAreExhausted() throws Exception {
                StoredFile scanningFile = StoredFile.restore(
                                FILE_ID,
                                "owner-1",
                                "report.pdf",
                                "application/pdf",
                                FileStatus.SCANNING,
                                12L,
                                SHA_256,
                                "quarantine/11111111-1111-1111-1111-111111111111/payload",
                                "version-1",
                                3,
                                STARTED_AT,
                                STARTED_AT,
                                null,
                                LEASE_ID,
                                STARTED_AT.plusSeconds(30),
                                null);
                when(repository.claimPendingScan(
                                eq(FILE_ID),
                                eq(LEASE_ID),
                                eq(STARTED_AT),
                                eq(STARTED_AT.plusSeconds(30))))
                                .thenReturn(Optional.of(scanningFile));
                when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));
                when(contentStorage.openStream(FILE_ID)).thenReturn(
                                new ByteArrayInputStream("safe content".getBytes()));
                when(antivirusScanner.scan(any(InputStream.class))).thenAnswer(invocation -> {
                        InputStream content = invocation.getArgument(0);
                        content.transferTo(OutputStream.nullOutputStream());
                        return AntivirusScanResult.retryableFailure("CLAMAV_UNAVAILABLE");
                });

                ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(FILE_ID));

                assertThat(result.status()).contains(FileStatus.SCAN_FAILED);
                ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
                ArgumentCaptor<ScanAttempt> scanAttempt = ArgumentCaptor.forClass(ScanAttempt.class);
                verify(repository).completeScan(storedFile.capture(), scanAttempt.capture());
                assertThat(storedFile.getValue().failureCode()).contains("SCAN_ATTEMPTS_EXHAUSTED");
                assertThat(scanAttempt.getValue().failureCode()).isEqualTo("CLAMAV_UNAVAILABLE");
        }

    @Test
    void scan_shouldFailClosed_whenMessageMetadataDoesNotMatchStoredFile() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
                LEASE_ID,
                STARTED_AT.plusSeconds(30),
                STARTED_AT);
        when(repository.claimPendingScan(
                eq(FILE_ID),
                eq(LEASE_ID),
                eq(STARTED_AT),
                eq(STARTED_AT.plusSeconds(30))))
                .thenReturn(Optional.of(scanningFile));

        ScanFileResult result = scanFileUseCase.scan(new ScanFileCommand(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                FILE_ID,
                12L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "quarantine/11111111-1111-1111-1111-111111111111/payload",
                "version-1"));

        assertThat(result.status()).contains(FileStatus.SCAN_FAILED);
        verifyNoInteractions(contentStorage, antivirusScanner);
        ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).completeScan(storedFile.capture(), any());
        assertThat(storedFile.getValue().failureCode()).contains("SCAN_MESSAGE_MISMATCH");
    }

    private StoredFile createPendingScanFile() {
        return StoredFile.startUpload(
                        FILE_ID,
                        "owner-1",
                        "report.pdf",
                        "application/pdf",
                        STARTED_AT)
                .completeUpload(
                        12L,
                        SHA_256,
                        new StorageReceipt(
                                "quarantine/11111111-1111-1111-1111-111111111111/payload",
                                "version-1"),
                        STARTED_AT);
    }
}
