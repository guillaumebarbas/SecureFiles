package com.securefiles.domain.file.model;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoredFileTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LEASE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final String SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Test
    void completeUpload_shouldTransitionToPendingScan_whenMetadataIsComplete() {
        StoredFile uploadingFile = StoredFile.startUpload(
                FILE_ID,
                "owner-1",
                "report.pdf",
                "application/pdf",
                CREATED_AT);

        StoredFile pendingScanFile = uploadingFile.completeUpload(
                12L,
                SHA_256,
                new StorageReceipt("quarantine/11111111-1111-1111-1111-111111111111/payload", "version-1"),
                CREATED_AT.plusSeconds(1));

        assertThat(pendingScanFile.status()).isEqualTo(FileStatus.PENDING_SCAN);
        assertThat(pendingScanFile.sizeBytes()).contains(12L);
        assertThat(pendingScanFile.sha256()).contains(SHA_256);
    }

    @Test
    void reject_shouldTransitionToRejected_whenTransferFails() {
        StoredFile uploadingFile = StoredFile.startUpload(
                FILE_ID,
                "owner-1",
                "report.pdf",
                "application/pdf",
                CREATED_AT);

        StoredFile rejectedFile = uploadingFile.reject("UPLOAD_FAILED", CREATED_AT.plusSeconds(1));

        assertThat(rejectedFile.status()).isEqualTo(FileStatus.REJECTED);
        assertThat(rejectedFile.failureCode()).contains("UPLOAD_FAILED");
    }

        @Test
        void claimForScan_shouldTransitionToScanningAndCreateLease_whenFileIsPendingScan() {
        StoredFile pendingScanFile = createPendingScanFile();

        StoredFile scanningFile = pendingScanFile.claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        assertThat(scanningFile.status()).isEqualTo(FileStatus.SCANNING);
        assertThat(scanningFile.scanAttemptCount()).isEqualTo(1);
        assertThat(scanningFile.scanLeaseId()).contains(LEASE_ID);
        assertThat(scanningFile.scanLeaseUntil()).contains(CREATED_AT.plusSeconds(30));
        }

        @Test
        void completeScan_shouldTransitionToClean_whenScannerReturnsCleanForActiveLease() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        StoredFile cleanFile = scanningFile.completeScan(
            LEASE_ID,
            AntivirusScanResult.clean(),
            CREATED_AT.plusSeconds(3));

        assertThat(cleanFile.status()).isEqualTo(FileStatus.CLEAN);
        assertThat(cleanFile.scanLeaseId()).isEmpty();
        assertThat(cleanFile.scanLeaseUntil()).isEmpty();
        }

        @Test
        void completeScan_shouldTransitionToInfected_whenScannerFindsThreat() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        StoredFile infectedFile = scanningFile.completeScan(
            LEASE_ID,
            AntivirusScanResult.infected(),
            CREATED_AT.plusSeconds(3));

        assertThat(infectedFile.status()).isEqualTo(FileStatus.INFECTED);
        }

        @Test
        void completeScan_shouldRejectResult_whenLeaseDoesNotMatch() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        assertThatThrownBy(() -> scanningFile.completeScan(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            AntivirusScanResult.clean(),
            CREATED_AT.plusSeconds(3)))
            .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void completeScan_shouldReturnToPendingScan_whenScannerFailsTemporarily() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        StoredFile pendingScanFile = scanningFile.completeScan(
            LEASE_ID,
            AntivirusScanResult.retryableFailure("CLAMAV_TIMEOUT"),
            CREATED_AT.plusSeconds(3),
            CREATED_AT.plusSeconds(60));

        assertThat(pendingScanFile.status()).isEqualTo(FileStatus.PENDING_SCAN);
        assertThat(pendingScanFile.failureCode()).contains("CLAMAV_TIMEOUT");
        assertThat(pendingScanFile.nextScanAt()).contains(CREATED_AT.plusSeconds(60));
        }

        @Test
        void completeScan_shouldTransitionToScanFailed_whenScannerFailsTerminally() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        StoredFile failedFile = scanningFile.completeScan(
            LEASE_ID,
            AntivirusScanResult.terminalFailure("SCAN_ATTEMPTS_EXHAUSTED"),
            CREATED_AT.plusSeconds(3));

        assertThat(failedFile.status()).isEqualTo(FileStatus.SCAN_FAILED);
        assertThat(failedFile.failureCode()).contains("SCAN_ATTEMPTS_EXHAUSTED");
        }

        @Test
        void recoverExpiredScan_shouldReturnToPendingScan_whenLeaseHasExpired() {
        StoredFile scanningFile = createPendingScanFile().claimForScan(
            LEASE_ID,
            CREATED_AT.plusSeconds(30),
            CREATED_AT.plusSeconds(2));

        StoredFile pendingScanFile = scanningFile.recoverExpiredScan(
            CREATED_AT.plusSeconds(31),
            CREATED_AT.plusSeconds(60));

        assertThat(pendingScanFile.status()).isEqualTo(FileStatus.PENDING_SCAN);
        assertThat(pendingScanFile.failureCode()).contains("SCAN_LEASE_EXPIRED");
        assertThat(pendingScanFile.scanLeaseId()).isEmpty();
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
                CREATED_AT.plusSeconds(1));
        }
}