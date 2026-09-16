package com.securefiles.domain.file.port.in;

public interface DownloadFile {

    DownloadFileResult download(DownloadFileCommand command);
}
