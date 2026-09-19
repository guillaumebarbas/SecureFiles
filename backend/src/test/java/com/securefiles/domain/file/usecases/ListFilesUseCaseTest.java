package com.securefiles.domain.file.usecases;

import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StoredFile;
import com.securefiles.domain.file.model.list.FileListQuery;
import com.securefiles.domain.file.model.list.FileSortField;
import com.securefiles.domain.file.model.list.ListFilesException;
import com.securefiles.domain.file.model.list.SortDirection;
import com.securefiles.domain.file.port.in.ListFilesCommand;
import com.securefiles.domain.file.port.in.ListFilesResult;
import com.securefiles.domain.file.port.out.StoredFilePage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListFilesUseCaseTest {

    private static final UUID NEWEST_FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OLDEST_FILE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NEWEST_CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");
    private static final Instant OLDEST_CREATED_AT = Instant.parse("2026-09-14T10:00:00Z");
    private static final String NEWEST_SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";
    private static final String OLDEST_SHA_256 = "88d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";
        private static final UUID FIRST_OWNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        private static final UUID SECOND_OWNER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private StoredFileRepository repository;

        @Mock
        private UserRepository userRepository;

    private ListFilesUseCase listFilesUseCase;

    @BeforeEach
    void setUp() {
                listFilesUseCase = new ListFilesUseCase(repository, userRepository);
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
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(newestFile, oldestFile), 2));

        ListFilesResult result = listFilesUseCase.list(new ListFilesCommand());

        assertThat(result.content())
                .extracting(metadata -> metadata.fileId())
                .containsExactly(NEWEST_FILE_ID, OLDEST_FILE_ID);
        assertThat(result.content().get(0).originalFilename()).isEqualTo("newest.pdf");
        assertThat(result.content().get(0).sizeBytes()).contains(42L);
        assertThat(result.content().get(0).status()).isEqualTo(FileStatus.PENDING_SCAN);
    }

        @Test
        void list_shouldReturnPageMetadata_whenRepositoryProvidesRequestedPage() {
                StoredFile oldestFile = createFile(
                                OLDEST_FILE_ID,
                                "oldest.pdf",
                                12L,
                                OLDEST_SHA_256,
                                FileStatus.CLEAN,
                                OLDEST_CREATED_AT);
                when(repository.findPage(new FileListQuery(2, 10))).thenReturn(new StoredFilePage(List.of(oldestFile), 11));

                ListFilesResult result = listFilesUseCase.list(new ListFilesCommand(2, 10));

                assertThat(result.content()).extracting(metadata -> metadata.fileId())
                                .containsExactly(OLDEST_FILE_ID);
                assertThat(result.page()).isEqualTo(2);
                assertThat(result.size()).isEqualTo(10);
                assertThat(result.totalElements()).isEqualTo(11);
                assertThat(result.totalPages()).isEqualTo(2);
                assertThat(result.hasNext()).isFalse();
                assertThat(result.hasPrevious()).isTrue();
        }

        @Test
        void list_shouldRequestTheSortedAndFilteredPage() {
                FileListQuery query = new FileListQuery(
                                1,
                                10,
                                FileSortField.NAME,
                                SortDirection.ASC,
                                Set.of(FileStatus.CLEAN, FileStatus.SCANNING));
                StoredFile cleanFile = createFile(
                                NEWEST_FILE_ID,
                                "clean.pdf",
                                12L,
                                NEWEST_SHA_256,
                                FileStatus.CLEAN,
                                NEWEST_CREATED_AT);
                when(repository.findPage(query)).thenReturn(new StoredFilePage(List.of(cleanFile), 1));

                ListFilesResult result = listFilesUseCase.list(new ListFilesCommand(query));

                assertThat(result.content()).singleElement()
                                .extracting(metadata -> metadata.originalFilename())
                                .isEqualTo("clean.pdf");
        }

        @Test
        void list_shouldRejectInvalidPageNumber() {
                Throwable thrown = catchThrowable(() -> new ListFilesCommand(0, 10));

                assertThat(thrown).isInstanceOf(ListFilesException.class);
                assertThat(((ListFilesException) thrown).code()).isEqualTo("INVALID_PAGINATION");
        }

        @Test
        void list_shouldRejectPageSizeAboveMaximum() {
                Throwable thrown = catchThrowable(() -> new ListFilesCommand(1, 51));

                assertThat(thrown).isInstanceOf(ListFilesException.class);
                assertThat(((ListFilesException) thrown).code()).isEqualTo("INVALID_PAGINATION");
        }

        @Test
        void listQuery_shouldRejectPageBeyondMaximumOffset() {
                Throwable thrown = catchThrowable(() -> new FileListQuery(1_002, 10));

                assertThat(thrown).isInstanceOf(ListFilesException.class);
                assertThat(((ListFilesException) thrown).code()).isEqualTo("INVALID_PAGINATION");
        }

        @Test
        void listQuery_shouldRejectDeletingStatus_whenRequestedByPublicList() {
                Throwable thrown = catchThrowable(() -> new FileListQuery(
                                1,
                                10,
                                FileSortField.CREATED_AT,
                                SortDirection.DESC,
                                Set.of(FileStatus.DELETING)));

                assertThat(thrown).isInstanceOf(ListFilesException.class);
                assertThat(((ListFilesException) thrown).code()).isEqualTo("INVALID_LIST_QUERY");
        }

    @Test
    void list_shouldReturnEmptyMetadata_whenRequesterHasNoFiles() {
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(), 0));

        ListFilesResult result = listFilesUseCase.list(new ListFilesCommand());

        assertThat(result.content()).isEmpty();
    }

    @Test
        void list_shouldHideFailureDiagnostics_whenScanAttemptsAreExhausted() {
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
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(failedFile, cleanFile), 2));

        ListFilesResult result = listFilesUseCase.list(new ListFilesCommand());

        assertThat(result.content().get(0).failureCode()).isEmpty();
        assertThat(result.content().get(0).failureCause()).isEmpty();
        assertThat(result.content().get(1).failureCode()).isEmpty();
        verify(repository, never()).findLatestPreciseFailureCodesByFileIds(Set.of(NEWEST_FILE_ID));
    }

    @Test
    void list_shouldReturnAllFilesWithResolvedAuthors_whenFilesHaveDifferentOwners() {
        StoredFile firstOwnerFile = createFileForOwner(
                FIRST_OWNER_ID.toString(),
                NEWEST_FILE_ID,
                "first-owner.pdf",
                42L,
                NEWEST_SHA_256,
                FileStatus.CLEAN,
                NEWEST_CREATED_AT);
        StoredFile secondOwnerFile = createFileForOwner(
                SECOND_OWNER_ID.toString(),
                OLDEST_FILE_ID,
                "second-owner.pdf",
                12L,
                OLDEST_SHA_256,
                FileStatus.CLEAN,
                OLDEST_CREATED_AT);
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(firstOwnerFile, secondOwnerFile), 2));
        when(userRepository.findById(FIRST_OWNER_ID)).thenReturn(Optional.of(createUser(FIRST_OWNER_ID, "Alice Martin")));
        when(userRepository.findById(SECOND_OWNER_ID)).thenReturn(Optional.of(createUser(SECOND_OWNER_ID, "Bob Dupont")));

        ListFilesResult result = listFilesUseCase.list(new ListFilesCommand());

        assertThat(result.content()).extracting(metadata -> metadata.originalFilename())
                .containsExactly("first-owner.pdf", "second-owner.pdf");
        assertThat(result.content()).extracting(metadata -> metadata.author())
                .containsExactly("Alice Martin", "Bob Dupont");
    }

    @Test
    void list_shouldUseNeutralAuthor_whenOwnerCannotBeResolved() {
        StoredFile legacyFile = createFileForOwner(
                "legacy-owner-1",
                NEWEST_FILE_ID,
                "legacy.pdf",
                42L,
                NEWEST_SHA_256,
                FileStatus.CLEAN,
                NEWEST_CREATED_AT);
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(legacyFile), 1));

        ListFilesResult result = listFilesUseCase.list(new ListFilesCommand());

        assertThat(result.content()).singleElement()
                .extracting(metadata -> metadata.author())
                .isEqualTo("Auteur inconnu");
    }

    @Test
    void list_shouldExposeActionCapabilities_forRequesterAndAdministrator() {
        StoredFile ownerFile = createFileForOwner(
                FIRST_OWNER_ID.toString(),
                NEWEST_FILE_ID,
                "owner.pdf",
                42L,
                NEWEST_SHA_256,
                FileStatus.CLEAN,
                NEWEST_CREATED_AT);
        StoredFile otherFile = createFileForOwner(
                SECOND_OWNER_ID.toString(),
                OLDEST_FILE_ID,
                "other.pdf",
                12L,
                OLDEST_SHA_256,
                FileStatus.CLEAN,
                OLDEST_CREATED_AT);
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(ownerFile, otherFile), 2));

        ListFilesResult ownerResult = listFilesUseCase.list(
                new ListFilesCommand(new FileListQuery(), FIRST_OWNER_ID.toString(), false));
        ListFilesResult administratorResult = listFilesUseCase.list(
                new ListFilesCommand(new FileListQuery(), "administrator-1", true));

        assertThat(ownerResult.content().get(0).canDownload()).isTrue();
        assertThat(ownerResult.content().get(0).canDelete()).isTrue();
        assertThat(ownerResult.content().get(1).canDownload()).isTrue();
        assertThat(ownerResult.content().get(1).canDelete()).isFalse();
        assertThat(administratorResult.content().get(0).canDownload()).isTrue();
        assertThat(administratorResult.content().get(1).canDownload()).isTrue();
        assertThat(administratorResult.content().get(0).canDelete()).isTrue();
        assertThat(administratorResult.content().get(1).canDelete()).isTrue();
    }

    @Test
    void list_shouldHideDownloadCapability_whenRequesterIsAnonymous() {
        StoredFile cleanFile = createFileForOwner(
                FIRST_OWNER_ID.toString(),
                NEWEST_FILE_ID,
                "clean.pdf",
                42L,
                NEWEST_SHA_256,
                FileStatus.CLEAN,
                NEWEST_CREATED_AT);
        when(repository.findPage(new FileListQuery())).thenReturn(new StoredFilePage(List.of(cleanFile), 1));

        ListFilesResult result = listFilesUseCase.list(new ListFilesCommand());

        assertThat(result.content()).singleElement()
                .extracting(metadata -> metadata.canDownload())
                .isEqualTo(false);
    }

    @Test
    void list_shouldHideDeleteCapability_whenFileIsDeleting() {
        StoredFile deletingFile = StoredFile.restore(
                NEWEST_FILE_ID,
                FIRST_OWNER_ID.toString(),
                "deleting.pdf",
                "application/pdf",
                FileStatus.DELETING,
                42L,
                NEWEST_SHA_256,
                "quarantine/" + NEWEST_FILE_ID + "/payload",
                "version-1",
                0,
                NEWEST_CREATED_AT,
                NEWEST_CREATED_AT,
                null,
                null,
                null,
                null);
        when(repository.findPage(new FileListQuery()))
                .thenReturn(new StoredFilePage(List.of(deletingFile), 1));

        ListFilesResult result = listFilesUseCase.list(
                new ListFilesCommand(new FileListQuery(), FIRST_OWNER_ID.toString(), false));

        assertThat(result.content()).singleElement()
                .extracting(metadata -> metadata.canDelete())
                .isEqualTo(false);
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

    private StoredFile createFileForOwner(
            String ownerId,
            UUID fileId,
            String filename,
            long sizeBytes,
            String sha256,
            FileStatus status,
            Instant createdAt) {
        StoredFile uploadingFile = StoredFile.startUpload(
                fileId,
                ownerId,
                filename,
                "application/pdf",
                createdAt);
        StoredFile pendingScanFile = uploadingFile.completeUpload(
                sizeBytes,
                sha256,
                new StorageReceipt("quarantine/" + fileId + "/payload", "version-1"),
                createdAt);
        if (status == FileStatus.PENDING_SCAN) {
            return pendingScanFile;
        }
        UUID leaseId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        return pendingScanFile.claimForScan(
                        leaseId,
                        createdAt.plus(5, ChronoUnit.MINUTES),
                        createdAt)
                .completeScan(
                        leaseId,
                        com.securefiles.domain.file.model.AntivirusScanResult.clean(),
                        createdAt.plus(1, ChronoUnit.MINUTES));
    }

    private User createUser(UUID userId, String name) {
        return User.create(
                userId,
                name,
                "password-hash",
                Set.of(UserRole.UTILISATEUR),
                NEWEST_CREATED_AT);
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