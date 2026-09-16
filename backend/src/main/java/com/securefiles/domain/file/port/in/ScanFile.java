package com.securefiles.domain.file.port.in;

public interface ScanFile {

    ScanFileResult scan(ScanFileCommand command);
}
