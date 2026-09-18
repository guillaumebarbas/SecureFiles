package com.securefiles.domain.file.port.in;

public interface DeleteFile {

    void delete(DeleteFileCommand command);
}