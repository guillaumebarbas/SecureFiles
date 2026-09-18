package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.download.DownloadException;
import com.securefiles.domain.file.port.in.DownloadFileCommand;
import com.securefiles.domain.file.port.in.DownloadFileResult;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadFileUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final String SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Mock
    private StoredFileRepository repository;

    @Mock
    private FileContentStorage contentStorage;

    private DownloadFileUseCase downloadFileUseCase;

    @BeforeEach
    void setUp() {
        downloadFileUseCase = new DownloadFileUseCase(repository, contentStorage);
    }

    @Test
    void download_shouldOpenContent_whenFileIsCleanAndOwnerMatches() {
        StoredFile cleanFile = createCleanFile();
        InputStream content = new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8));
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(cleanFile));
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));
        when(contentStorage.openStream(FILE_ID)).thenReturn(content);

        DownloadFileResult result = downloadFileUseCase.download(
                new DownloadFileCommand(FILE_ID, "owner-1"));

        assertThat(result.fileId()).isEqualTo(FILE_ID);
        assertThat(result.originalFilename()).isEqualTo("report.pdf");
        assertThat(result.contentType()).contains("application/pdf");
        assertThat(result.content()).isSameAs(content);
        verify(contentStorage).openStream(FILE_ID);
    }

        @Test
        void download_shouldOpenContent_whenFileIsCleanAndAuthenticatedRequesterIsNotOwner() {
                StoredFile cleanFile = createCleanFile();
                InputStream content = new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8));
                when(repository.findById(FILE_ID)).thenReturn(Optional.of(cleanFile));
                when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));
                when(contentStorage.openStream(FILE_ID)).thenReturn(content);

                DownloadFileResult result = downloadFileUseCase.download(
                                new DownloadFileCommand(FILE_ID, "other-owner"));

                assertThat(result.fileId()).isEqualTo(FILE_ID);
                assertThat(result.content()).isSameAs(content);
                verify(contentStorage).openStream(FILE_ID);
        }

    @Test
    void download_shouldReject_whenFileIsNotClean() {
        StoredFile pendingScanFile = createPendingScanFile();
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(pendingScanFile));

        assertThatThrownBy(() -> downloadFileUseCase.download(
                new DownloadFileCommand(FILE_ID, "owner-1")))
                .isInstanceOf(DownloadException.class)
                .extracting(exception -> ((DownloadException) exception).code())
                .isEqualTo("FILE_NOT_AVAILABLE");

        verifyNoInteractions(contentStorage);
    }

    @Test
    void download_shouldReject_whenStorageMetadataDoesNotMatch() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createCleanFile()));
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "different-version"));

        assertThatThrownBy(() -> downloadFileUseCase.download(
                new DownloadFileCommand(FILE_ID, "owner-1")))
                .isInstanceOf(DownloadException.class)
                .extracting(exception -> ((DownloadException) exception).code())
                .isEqualTo("STORAGE_INTEGRITY_MISMATCH");

        verify(contentStorage, never()).openStream(FILE_ID);
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
        return createPendingScanFile()
                .claimForScan(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        CREATED_AT.plusSeconds(30),
                        CREATED_AT)
                .completeScan(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        com.securefiles.domain.file.model.AntivirusScanResult.clean(),
                        CREATED_AT.plusSeconds(1));
    }
}
