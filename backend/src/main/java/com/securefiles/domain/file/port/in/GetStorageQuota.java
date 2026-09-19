package com.securefiles.domain.file.port.in;

public interface GetStorageQuota {

    GetStorageQuotaResult get(GetStorageQuotaCommand command);
}