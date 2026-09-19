package com.securefiles.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.securefiles.application.dto.StorageQuotaResponseDto;
import com.securefiles.domain.file.port.in.GetStorageQuotaResult;
import org.junit.jupiter.api.Test;

class StorageQuotaMapperTest {

    private final StorageQuotaMapper mapper = new StorageQuotaMapper();

    @Test
    void toResponse_shouldExposeStorageUsageAndQuota() {
        StorageQuotaResponseDto response = mapper.toResponse(new GetStorageQuotaResult(4_000L, 10_000L));

        assertThat(response.usedBytes()).isEqualTo(4_000L);
        assertThat(response.quotaBytes()).isEqualTo(10_000L);
    }
}