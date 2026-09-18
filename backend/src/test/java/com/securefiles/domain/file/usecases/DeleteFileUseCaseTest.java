package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.AntivirusScanResult;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.delete.DeleteFileException;
import com.securefiles.domain.file.port.in.DeleteFileCommand;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteFileUseCaseTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LEASE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final String SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Mock
    private StoredFileRepository repository;

    @Mock
    private FileContentStorage contentStorage;

    private DeleteFileUseCase deleteFileUseCase;

    @BeforeEach
    void setUp() {
        deleteFileUseCase = new DeleteFileUseCase(repository, contentStorage);
    }

    @Test
    void delete_shouldRemoveContentAndMetadata_whenOwnerRequestsDeletion() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createFile("owner-1", FileStatus.CLEAN)));

        deleteFileUseCase.delete(new DeleteFileCommand(FILE_ID, "owner-1", false));

        verify(contentStorage).delete(FILE_ID);
        verify(repository).delete(FILE_ID);
    }

    @Test
    void delete_shouldRemoveContentAndMetadata_whenAdministratorRequestsDeletion() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createFile("owner-1", FileStatus.CLEAN)));

        deleteFileUseCase.delete(new DeleteFileCommand(FILE_ID, "administrator-1", true));

        verify(contentStorage).delete(FILE_ID);
        verify(repository).delete(FILE_ID);
    }

    @Test
    void delete_shouldHideFileExistence_whenRequesterIsNotOwnerOrAdministrator() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createFile("owner-1", FileStatus.CLEAN)));

        assertThatThrownBy(() -> deleteFileUseCase.delete(
                new DeleteFileCommand(FILE_ID, "other-user", false)))
                .isInstanceOf(DeleteFileException.class)
                .extracting(exception -> ((DeleteFileException) exception).code())
                .isEqualTo("FILE_NOT_FOUND");

        verifyNoInteractions(contentStorage);
    }

    @Test
    void delete_shouldReject_whenFileIsBeingScanned() {
        when(repository.findById(FILE_ID)).thenReturn(Optional.of(createFile("owner-1", FileStatus.SCANNING)));

        assertThatThrownBy(() -> deleteFileUseCase.delete(
                new DeleteFileCommand(FILE_ID, "owner-1", false)))
                .isInstanceOf(DeleteFileException.class)
                .extracting(exception -> ((DeleteFileException) exception).code())
                .isEqualTo("FILE_NOT_AVAILABLE");

        verify(contentStorage, never()).delete(FILE_ID);
        verify(repository, never()).delete(FILE_ID);
    }

    private StoredFile createFile(String ownerId, FileStatus status) {
        StoredFile pendingScanFile = StoredFile.startUpload(
                        FILE_ID,
                        ownerId,
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

        if (status == FileStatus.PENDING_SCAN) {
            return pendingScanFile;
        }

        if (status == FileStatus.SCANNING) {
            return pendingScanFile.claimForScan(LEASE_ID, CREATED_AT.plusSeconds(30), CREATED_AT);
        }

        return pendingScanFile
                .claimForScan(LEASE_ID, CREATED_AT.plusSeconds(30), CREATED_AT)
                .completeScan(
                        LEASE_ID,
                        status == FileStatus.CLEAN
                                ? AntivirusScanResult.clean()
                                : AntivirusScanResult.terminalFailure("SCAN_FAILED"),
                        CREATED_AT.plusSeconds(1));
    }
}