package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.model.StorageMetadata;
import java.io.InputStream;
import java.util.UUID;

public interface FileContentStorage {

    /**
     * Consumes the input stream completely before returning a receipt.
     */
    StorageReceipt store(UUID fileId, InputStream content);

    StorageMetadata head(UUID fileId);

    InputStream openStream(UUID fileId);

    void delete(UUID fileId);
}
