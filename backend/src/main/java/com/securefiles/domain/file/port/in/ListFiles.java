package com.securefiles.domain.file.port.in;

public interface ListFiles {

    ListFilesResult list(ListFilesCommand command);
}