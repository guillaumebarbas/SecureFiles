package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.FileScanRequested;
import com.securefiles.domain.file.model.StoredFile;

public interface FileAcceptancePort {

    /**
     * Atomically accepts the completed file and records its scan request.
     */
    void accept(StoredFile storedFile, FileScanRequested scanRequest);
}
