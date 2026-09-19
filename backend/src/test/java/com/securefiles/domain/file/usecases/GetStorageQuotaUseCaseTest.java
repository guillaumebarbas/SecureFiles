package com.securefiles.domain.file.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securefiles.domain.file.model.StorageQuota;
import com.securefiles.domain.file.port.in.GetStorageQuotaCommand;
import com.securefiles.domain.file.port.in.GetStorageQuotaResult;
import com.securefiles.domain.file.port.out.StorageQuotaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStorageQuotaUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final long DEFAULT_QUOTA_BYTES = 10_000L;

    @Mock
    private StorageQuotaRepository repository;

    private GetStorageQuotaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStorageQuotaUseCase(repository, DEFAULT_QUOTA_BYTES);
    }

    @Test
    void get_shouldReturnStoredUsage_whenOwnerHasQuota() {
        when(repository.findByOwnerId(USER_ID.toString()))
                .thenReturn(Optional.of(new StorageQuota(USER_ID.toString(), 4_000L, 10_000L)));

        GetStorageQuotaResult result = useCase.get(new GetStorageQuotaCommand(USER_ID));

        assertThat(result.usedBytes()).isEqualTo(4_000L);
        assertThat(result.quotaBytes()).isEqualTo(10_000L);
        verify(repository).findByOwnerId(USER_ID.toString());
    }

    @Test
    void get_shouldUseDefaultQuota_whenOwnerHasNoStoredQuota() {
        when(repository.findByOwnerId(USER_ID.toString())).thenReturn(Optional.empty());

        GetStorageQuotaResult result = useCase.get(new GetStorageQuotaCommand(USER_ID));

        assertThat(result.usedBytes()).isZero();
        assertThat(result.quotaBytes()).isEqualTo(DEFAULT_QUOTA_BYTES);
        verify(repository).findByOwnerId(USER_ID.toString());
    }
}