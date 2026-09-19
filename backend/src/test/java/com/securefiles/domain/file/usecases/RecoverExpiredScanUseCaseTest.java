package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.port.in.RecoverExpiredScanCommand;
import com.securefiles.domain.file.port.in.RecoverExpiredScanResult;
import com.securefiles.domain.file.port.out.ExpiredScanRecoveryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoverExpiredScanUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    @Mock
        private ExpiredScanRecoveryPort recoveryPort;

    private RecoverExpiredScanUseCase recoverExpiredScanUseCase;

    @BeforeEach
    void setUp() {
        recoverExpiredScanUseCase = new RecoverExpiredScanUseCase(
                recoveryPort,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> EVENT_ID,
                Duration.ofSeconds(60));
    }

    @Test
    void recover_shouldReturnPendingScan_whenRepositoryRecoversExpiredLease() {
        StoredFile pendingScanFile = StoredFile.startUpload(
                        FILE_ID,
                        "owner-1",
                        "report.pdf",
                        "application/pdf",
                        NOW)
                .completeUpload(
                        12L,
                        "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b",
                        new StorageReceipt("quarantine/file/payload", "version-1"),
                        NOW.minusSeconds(120))
                .claimForScan(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        NOW.minusSeconds(60),
                        NOW.minusSeconds(120));
        StoredFile recoveredFile = pendingScanFile.recoverExpiredScan(NOW, NOW.plusSeconds(60));
        when(recoveryPort.recoverExpiredScan(
                        eq(FILE_ID), eq(EVENT_ID), eq(NOW), eq(NOW.plusSeconds(60))))
                .thenReturn(Optional.of(recoveredFile));

        RecoverExpiredScanResult result = recoverExpiredScanUseCase.recover(
                new RecoverExpiredScanCommand(FILE_ID));

        assertThat(result.recovered()).isTrue();
        assertThat(result.status()).contains(FileStatus.PENDING_SCAN);
        verify(recoveryPort).recoverExpiredScan(FILE_ID, EVENT_ID, NOW, NOW.plusSeconds(60));
    }

    @Test
    void recover_shouldReturnUnchanged_whenRepositoryCannotRecoverLease() {
        when(recoveryPort.recoverExpiredScan(
                        eq(FILE_ID), eq(EVENT_ID), eq(NOW), eq(NOW.plusSeconds(60))))
                .thenReturn(Optional.empty());

        RecoverExpiredScanResult result = recoverExpiredScanUseCase.recover(
                new RecoverExpiredScanCommand(FILE_ID));

        assertThat(result.recovered()).isFalse();
        assertThat(result.status()).isEmpty();
        verify(recoveryPort).recoverExpiredScan(FILE_ID, EVENT_ID, NOW, NOW.plusSeconds(60));
    }
}
