package com.securefiles.domain.file.port.in;

public interface GetFileMetadata {

    GetFileMetadataResult get(GetFileMetadataCommand command);
}