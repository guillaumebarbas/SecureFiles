package com.securefiles.application.mapper;

import com.securefiles.application.dto.FileMetadataResponseDto;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.port.in.GetFileMetadataResult;
import com.securefiles.domain.file.port.in.ListFilesResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileMetadataMapperTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");

    private final FileMetadataMapper mapper = new FileMetadataMapper();

    @Test
    void toResponse_shouldMapDomainMetadataToApplicationResponse() {
        GetFileMetadataResult result = new GetFileMetadataResult(
                FILE_ID,
                "report.pdf",
                "Alice Martin",
                Optional.of(42L),
                FileStatus.SCANNING,
                CREATED_AT,
                Optional.empty());

        FileMetadataResponseDto response = mapper.toResponse(result);

        assertThat(response.fileId()).isEqualTo(FILE_ID);
        assertThat(response.originalFilename()).isEqualTo("report.pdf");
        assertThat(response.author()).isEqualTo("Alice Martin");
        assertThat(response.sizeBytes()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo("SCANNING");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void toResponses_shouldMapMetadataListToApplicationResponses() {
        GetFileMetadataResult firstResult = new GetFileMetadataResult(
                FILE_ID,
                "report.pdf",
                "Alice Martin",
                Optional.of(42L),
                FileStatus.PENDING_SCAN,
                CREATED_AT,
                Optional.empty());
        GetFileMetadataResult secondResult = new GetFileMetadataResult(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "image.png",
                "Bob Dupont",
                Optional.empty(),
                FileStatus.UPLOADING,
                CREATED_AT,
                Optional.empty());

        List<FileMetadataResponseDto> responses = mapper.toResponses(List.of(firstResult, secondResult));

        assertThat(responses).extracting(response -> response.originalFilename())
                .containsExactly("report.pdf", "image.png");
        assertThat(responses.get(0).status()).isEqualTo("PENDING_SCAN");
        assertThat(responses.get(1).sizeBytes()).isNull();
    }

        @Test
        void toResponse_shouldMapFailureCode_whenMetadataContainsFailure() {
                GetFileMetadataResult result = new GetFileMetadataResult(
                                FILE_ID,
                                "installer.pkg",
                                "Alice Martin",
                                Optional.of(42L),
                                FileStatus.SCAN_FAILED,
                                CREATED_AT,
                                Optional.of("SCAN_ATTEMPTS_EXHAUSTED"),
                                Optional.of("CLAMAV_UNAVAILABLE"));

                FileMetadataResponseDto response = mapper.toResponse(result);

                assertThat(response.failureCode()).isEqualTo("SCAN_ATTEMPTS_EXHAUSTED");
                assertThat(response.failureCause()).isEqualTo("CLAMAV_UNAVAILABLE");
        }

    @Test
    void toPageResponse_shouldMapContentAndPaginationMetadata() {
        GetFileMetadataResult result = new GetFileMetadataResult(
                FILE_ID,
                "report.pdf",
                "Alice Martin",
                Optional.of(42L),
                FileStatus.CLEAN,
                CREATED_AT,
                Optional.empty());

        var response = mapper.toPageResponse(
                new ListFilesResult(List.of(result), 2, 10, 11, 2, false, true));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).originalFilename()).isEqualTo("report.pdf");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isTrue();
    }
}