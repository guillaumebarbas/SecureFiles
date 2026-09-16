package com.securefiles.domain.file.port.in;

public interface RecoverExpiredScan {

    RecoverExpiredScanResult recover(RecoverExpiredScanCommand command);
}