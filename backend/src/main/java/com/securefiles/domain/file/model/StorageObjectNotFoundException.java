package com.securefiles.domain.file.model;

public final class StorageObjectNotFoundException extends StorageIntegrityException {

    public StorageObjectNotFoundException() {
        super(FileFailureCodes.STORAGE_OBJECT_NOT_FOUND, "The stored object does not exist.");
    }
}