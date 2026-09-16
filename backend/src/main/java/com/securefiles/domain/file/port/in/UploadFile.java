package com.securefiles.domain.file.port.in;

import java.io.InputStream;

public interface UploadFile {

    UploadFileResult upload(UploadFileCommand command, InputStream content);
}
