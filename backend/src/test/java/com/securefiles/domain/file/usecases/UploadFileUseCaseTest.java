package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StorageObjectNotFoundException;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.upload.UploadException;
import com.securefiles.domain.file.port.in.UploadFileCommand;
import com.securefiles.domain.file.port.in.UploadFileResult;
import com.securefiles.domain.file.port.out.FileAcceptancePort;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadFileUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final StorageReceipt STORAGE_RECEIPT = new StorageReceipt(
            "quarantine/11111111-1111-1111-1111-111111111111/payload",
            "version-1");

    @Mock
    private StoredFileRepository repository;

    @Mock
    private FileContentStorage contentStorage;

    @Mock
    private FileAcceptancePort acceptancePort;

    private UploadFileUseCase uploadFileUseCase;

    @BeforeEach
    void setUp() {
        uploadFileUseCase = new UploadFileUseCase(
                repository,
                contentStorage,
                acceptancePort,
                Clock.fixed(CREATED_AT, ZoneOffset.UTC),
                () -> FILE_ID);
    }

    @Test
    void upload_shouldReturnPendingScanAndRequestScan_whenContentStorageConsumesStream() throws Exception {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(1);
            content.transferTo(OutputStream.nullOutputStream());
            return STORAGE_RECEIPT;
        });
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));

        UploadFileResult result = uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
            new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<StoredFile> storedFile = ArgumentCaptor.forClass(StoredFile.class);
        ArgumentCaptor<FileScanRequested> scanRequest = ArgumentCaptor.forClass(FileScanRequested.class);
        verify(acceptancePort).accept(storedFile.capture(), scanRequest.capture());
        assertThat(result.status()).isEqualTo(FileStatus.PENDING_SCAN);
        assertThat(result.sizeBytes()).isEqualTo(12L);
        assertThat(storedFile.getValue().status()).isEqualTo(FileStatus.PENDING_SCAN);
        assertThat(scanRequest.getValue().fileId()).isEqualTo(FILE_ID);
    }

    @Test
    void upload_shouldRejectBeforeStorage_whenFilenameIsUnsafe() {
        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "../report.pdf", "application/pdf"),
                new ByteArrayInputStream(new byte[] {1, 2, 3})))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("INVALID_FILENAME");

        verifyNoInteractions(repository, contentStorage, acceptancePort);
    }

    @Test
    void upload_shouldRejectAndCleanup_whenDeclaredSizeDoesNotMatchTransferredSize() throws Exception {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(1);
            content.transferTo(OutputStream.nullOutputStream());
            return STORAGE_RECEIPT;
        });
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(12L, "version-1"));

        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf", 99L),
            new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("DECLARED_SIZE_MISMATCH");

        verify(contentStorage).delete(FILE_ID);
        verify(acceptancePort, never()).accept(any(), any());
        verify(repository, times(2)).save(any(StoredFile.class));
    }

    @Test
    void upload_shouldRejectAndCleanup_whenContentStorageFails() {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class)))
                .thenThrow(new IllegalStateException("simulated storage failure"));

        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
                new ByteArrayInputStream(new byte[] {1, 2, 3})))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("UPLOAD_FAILED");

        verify(contentStorage).delete(FILE_ID);
        verify(acceptancePort, never()).accept(any(), any());
        verify(repository, times(2)).save(any(StoredFile.class));
    }

    @Test
    void upload_shouldRejectAndCleanup_whenContentStorageDoesNotConsumeTheWholeStream() {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenReturn(STORAGE_RECEIPT);

        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
                new ByteArrayInputStream(new byte[] {1, 2, 3})))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("INCOMPLETE_STREAM");

        verify(contentStorage).delete(FILE_ID);
        verify(acceptancePort, never()).accept(any(), any());
        verify(repository, times(2)).save(any(StoredFile.class));
    }

    @Test
    void upload_shouldRejectAndCleanup_whenStoredObjectSizeDiffersFromTransferredSize() throws Exception {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(1);
            content.transferTo(OutputStream.nullOutputStream());
            return STORAGE_RECEIPT;
        });
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(13L, "version-1"));

        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
                new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("STORAGE_SIZE_MISMATCH");

        verify(contentStorage).delete(FILE_ID);
        verify(acceptancePort, never()).accept(any(), any());
    }

    @Test
    void upload_shouldRejectAndCleanupWithObjectNotFoundCode_whenStoredObjectIsMissing() throws Exception {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(1);
            content.transferTo(OutputStream.nullOutputStream());
            return STORAGE_RECEIPT;
        });
        when(contentStorage.head(FILE_ID)).thenThrow(new StorageObjectNotFoundException());

        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
                new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("STORAGE_OBJECT_NOT_FOUND");

        verify(contentStorage).delete(FILE_ID);
        verify(acceptancePort, never()).accept(any(), any());
    }

    @Test
    void upload_shouldRejectAndCleanup_whenAcceptanceFails() throws Exception {
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(1);
            content.transferTo(OutputStream.nullOutputStream());
            return STORAGE_RECEIPT;
        });
        when(contentStorage.head(FILE_ID)).thenReturn(new StorageMetadata(3L, "version-1"));
        doThrow(new IllegalStateException("simulated acceptance failure"))
                .when(acceptancePort)
                .accept(any(StoredFile.class), any(FileScanRequested.class));

        assertThatThrownBy(() -> uploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
                new ByteArrayInputStream(new byte[] {1, 2, 3})))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("UPLOAD_FAILED");

        verify(contentStorage).delete(FILE_ID);
        verify(repository, times(2)).save(any(StoredFile.class));
    }

    @Test
    void upload_shouldRejectAndCleanup_whenMaximumSizeIsExceededDuringStreaming() {
        UploadFileUseCase limitedUploadFileUseCase = new UploadFileUseCase(
                repository,
                contentStorage,
                acceptancePort,
                Clock.fixed(CREATED_AT, ZoneOffset.UTC),
                () -> FILE_ID,
                5L);
        when(contentStorage.store(eq(FILE_ID), any(InputStream.class))).thenAnswer(invocation -> {
            InputStream content = invocation.getArgument(1);
            content.transferTo(OutputStream.nullOutputStream());
            return STORAGE_RECEIPT;
        });

        assertThatThrownBy(() -> limitedUploadFileUseCase.upload(
                new UploadFileCommand("owner-1", "report.pdf", "application/pdf"),
                new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("MAX_SIZE_EXCEEDED");

        verify(contentStorage).delete(FILE_ID);
        verify(acceptancePort, never()).accept(any(), any());
    }
}