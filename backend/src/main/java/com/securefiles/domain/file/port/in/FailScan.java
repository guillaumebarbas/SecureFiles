package com.securefiles.domain.file.port.in;

public interface FailScan {

    FailScanResult fail(FailScanCommand command);
}
