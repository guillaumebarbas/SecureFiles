package com.securefiles.domain.file.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securefiles.domain.file.model.AntivirusVerdict;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.ScanAttempt;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.FailScanCommand;
import com.securefiles.domain.file.port.in.FailScanResult;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Clock;
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

@ExtendWith(MockitoExtension.class)
class FailScanUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FAILED_AT = Instant.parse("2026-09-19T10:00:00Z");
    private static final String SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private StoredFileRepository repository;

    private FailScanUseCase failScanUseCase;

    @BeforeEach
    void setUp() {
        failScanUseCase = new FailScanUseCase(
                repository,
                Clock.fixed(FAILED_AT, ZoneOffset.UTC));
    }

    @Test
    void fail_shouldMarkPendingScanAsFailed_whenTransportRetriesAreExhausted() {
        StoredFile pendingFile = StoredFile.startUpload(
                        FILE_ID,
                        "owner-1",
                        "document.pdf",
                        "application/pdf",
                        FAILED_AT)
                .completeUpload(
                        4,
                        SHA256,
                        new StorageReceipt("quarantine/document/payload", "version-1"),
                        FAILED_AT);
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(pendingFile));
        when(repository.failPendingScan(any(StoredFile.class), any(ScanAttempt.class)))
                .thenReturn(true);

        FailScanResult result = failScanUseCase.fail(new FailScanCommand(
                FILE_ID,
                FileFailureCodes.RABBITMQ_TRANSPORT_EXHAUSTED));

        assertThat(result.failed()).isTrue();
        assertThat(result.status()).contains(FileStatus.SCAN_FAILED);
        ArgumentCaptor<StoredFile> fileCaptor = ArgumentCaptor.forClass(StoredFile.class);
        ArgumentCaptor<ScanAttempt> attemptCaptor = ArgumentCaptor.forClass(ScanAttempt.class);
        verify(repository).failPendingScan(fileCaptor.capture(), attemptCaptor.capture());
        assertThat(fileCaptor.getValue().failureCode())
                .contains(FileFailureCodes.SCAN_ATTEMPTS_EXHAUSTED);
        assertThat(attemptCaptor.getValue().verdict()).isEqualTo(AntivirusVerdict.TERMINAL_FAILURE);
        assertThat(attemptCaptor.getValue().failureCode())
                .isEqualTo(FileFailureCodes.RABBITMQ_TRANSPORT_EXHAUSTED);
    }

        @Test
        void fail_shouldNotPersistFailure_whenFileDoesNotExist() {
                when(repository.findById(FILE_ID)).thenReturn(Optional.empty());

                FailScanResult result = failScanUseCase.fail(new FailScanCommand(
                                FILE_ID,
                                FileFailureCodes.RABBITMQ_TRANSPORT_EXHAUSTED));

                assertThat(result.failed()).isFalse();
                assertThat(result.status()).isEmpty();
                verify(repository, never()).failPendingScan(any(StoredFile.class), any(ScanAttempt.class));
        }

        @Test
        void fail_shouldNotPersistFailure_whenFileIsAlreadyTerminal() {
                StoredFile cleanFile = StoredFile.restore(
                                FILE_ID,
                                "owner-1",
                                "document.pdf",
                                "application/pdf",
                                FileStatus.CLEAN,
                                4L,
                                SHA256,
                                "quarantine/document/payload",
                                "version-1",
                                1,
                                FAILED_AT,
                                FAILED_AT,
                                null,
                                null,
                                null,
                                null);
                when(repository.findById(FILE_ID)).thenReturn(Optional.of(cleanFile));

                FailScanResult result = failScanUseCase.fail(new FailScanCommand(
                                FILE_ID,
                                FileFailureCodes.RABBITMQ_TRANSPORT_EXHAUSTED));

                assertThat(result.failed()).isFalse();
                assertThat(result.status()).contains(FileStatus.CLEAN);
                verify(repository, never()).failPendingScan(any(StoredFile.class), any(ScanAttempt.class));
        }
}
