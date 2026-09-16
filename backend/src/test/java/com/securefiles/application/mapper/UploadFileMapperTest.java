package com.securefiles.application.mapper;

import com.securefiles.application.dto.UploadFileRequestDto;
import com.securefiles.application.dto.UploadFileResponseDto;
import com.securefiles.domain.file.model.FileStatus;
import com.securefiles.domain.file.port.in.UploadFileCommand;
import com.securefiles.domain.file.port.in.UploadFileResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadFileMapperTest {

    private static final UUID FILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-09-15T10:00:00Z");

    private final UploadFileMapper mapper = new UploadFileMapper();

    @Test
    void toCommand_shouldMapApplicationRequestToDomainCommand() {
        var request = new UploadFileRequestDto(
                "owner-1",
                "report.pdf",
                "application/pdf",
                42L);

        UploadFileCommand result = mapper.toCommand(request);

        assertThat(result).isEqualTo(new UploadFileCommand(
                "owner-1",
                "report.pdf",
                "application/pdf",
                42L));
    }

    @Test
    void toResponse_shouldMapDomainResultToApplicationResponse() {
        var result = new UploadFileResult(
                FILE_ID,
                "report.pdf",
                42L,
                FileStatus.PENDING_SCAN,
                CREATED_AT);

        UploadFileResponseDto response = mapper.toResponse(result);

        assertThat(response.fileId()).isEqualTo(FILE_ID);
        assertThat(response.originalFilename()).isEqualTo("report.pdf");
        assertThat(response.sizeBytes()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo("PENDING_SCAN");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }
}