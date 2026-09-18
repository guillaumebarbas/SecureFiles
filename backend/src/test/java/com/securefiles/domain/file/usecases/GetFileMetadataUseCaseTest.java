package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.metadata.FileMetadataException;
import com.securefiles.domain.file.port.in.GetFileMetadataCommand;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFileMetadataUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final String SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Mock
    private StoredFileRepository repository;

    @Mock
    private UserRepository userRepository;

    private GetFileMetadataUseCase getFileMetadataUseCase;

    @BeforeEach
    void setUp() {
        getFileMetadataUseCase = new GetFileMetadataUseCase(repository, userRepository);
    }

    @Test
    void get_shouldReturnMetadata_whenRequesterOwnsFile() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createPendingScanFile()));

        GetFileMetadataResult result = getFileMetadataUseCase.get(
                new GetFileMetadataCommand(FILE_ID, "owner-1"));

        assertThat(result.fileId()).isEqualTo(FILE_ID);
        assertThat(result.originalFilename()).isEqualTo("report.pdf");
        assertThat(result.sizeBytes()).contains(12L);
        assertThat(result.status()).isEqualTo(FileStatus.PENDING_SCAN);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void get_shouldExposeOwnerCapabilities_whenFileIsClean() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createCleanFile()));

        GetFileMetadataResult result = getFileMetadataUseCase.get(
                new GetFileMetadataCommand(FILE_ID, "owner-1"));

        assertThat(result.canDownload()).isTrue();
        assertThat(result.canDelete()).isTrue();
    }

    @Test
    void get_shouldExposeAttemptsExhaustionAndPreciseCause_whenScanAttemptsAreExhausted() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createScanFailedFile()));
        when(repository.findLatestPreciseFailureCodesByFileIds(Set.of(FILE_ID)))
                .thenReturn(Map.of(FILE_ID, "CLAMAV_UNAVAILABLE"));

        GetFileMetadataResult result = getFileMetadataUseCase.get(
                new GetFileMetadataCommand(FILE_ID, "owner-1"));

        assertThat(result.failureCode()).contains("SCAN_ATTEMPTS_EXHAUSTED");
        assertThat(result.failureCause()).contains("CLAMAV_UNAVAILABLE");
    }

        @Test
        void get_shouldKeepStoredFailureCode_whenFailureIsNotAttemptsExhausted() {
        StoredFile scanFailedFile = createPendingScanFile()
            .claimForScan(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                CREATED_AT.plusSeconds(30),
                CREATED_AT)
            .completeScan(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                com.securefiles.domain.file.model.AntivirusScanResult
                    .terminalFailure("SCAN_MESSAGE_MISMATCH"),
                CREATED_AT.plusSeconds(1));
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(scanFailedFile));

        GetFileMetadataResult result = getFileMetadataUseCase.get(
            new GetFileMetadataCommand(FILE_ID, "owner-1"));

        assertThat(result.failureCode()).contains("SCAN_MESSAGE_MISMATCH");
        verify(repository, never()).findLatestPreciseFailureCodesByFileIds(Set.of(FILE_ID));
        }

    @Test
    void get_shouldHideFileExistence_whenRequesterDoesNotOwnFile() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createPendingScanFile()));

        assertThatThrownBy(() -> getFileMetadataUseCase.get(
                new GetFileMetadataCommand(FILE_ID, "other-owner")))
                .isInstanceOf(FileMetadataException.class)
                .extracting(exception -> ((FileMetadataException) exception).code())
                .isEqualTo("FILE_NOT_FOUND");
    }

    @Test
    void get_shouldReturnNotFound_whenFileDoesNotExist() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getFileMetadataUseCase.get(
                new GetFileMetadataCommand(FILE_ID, "owner-1")))
                .isInstanceOf(FileMetadataException.class)
                .extracting(exception -> ((FileMetadataException) exception).code())
                .isEqualTo("FILE_NOT_FOUND");
    }

    private StoredFile createPendingScanFile() {
        return StoredFile.startUpload(
                        FILE_ID,
                        "owner-1",
                        "report.pdf",
                        "application/pdf",
                        CREATED_AT)
                .completeUpload(
                        12L,
                        SHA_256,
                        new StorageReceipt(
                                "quarantine/11111111-1111-1111-1111-111111111111/payload",
                                "version-1"),
                        CREATED_AT);
    }

    private StoredFile createCleanFile() {
        UUID leaseId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        return createPendingScanFile()
                .claimForScan(leaseId, CREATED_AT.plusSeconds(30), CREATED_AT)
                .completeScan(
                        leaseId,
                        com.securefiles.domain.file.model.AntivirusScanResult.clean(),
                        CREATED_AT.plusSeconds(1));
    }

                        private StoredFile createScanFailedFile() {
                        UUID leaseId = UUID.fromString("22222222-2222-2222-2222-222222222222");
                        return createPendingScanFile()
                            .claimForScan(leaseId, CREATED_AT.plusSeconds(30), CREATED_AT)
                            .completeScan(
                                leaseId,
                                com.securefiles.domain.file.model.AntivirusScanResult
                                    .terminalFailure("SCAN_ATTEMPTS_EXHAUSTED"),
                                CREATED_AT.plusSeconds(1));
                        }
}