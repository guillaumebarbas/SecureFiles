package com.securefiles.domain.file.port.in;

import java.util.List;

public interface ListFiles {

    List<GetFileMetadataResult> list(ListFilesCommand command);
}