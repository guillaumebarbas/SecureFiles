package com.securefiles.infrastructure.clamav;

import java.io.IOException;

final class ClamAvWriteTimeoutException extends IOException {

    ClamAvWriteTimeoutException() {
        super("The ClamAV stream write timed out");
    }
}