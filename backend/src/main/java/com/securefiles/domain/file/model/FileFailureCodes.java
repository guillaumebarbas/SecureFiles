package com.securefiles.domain.file.model;

public final class FileFailureCodes {

    public static final String ANTIVIRUS_UNAVAILABLE = "ANTIVIRUS_UNAVAILABLE";
    public static final String CLAMAV_INVALID_RESPONSE = "CLAMAV_INVALID_RESPONSE";
    public static final String CLAMAV_UNAVAILABLE = "CLAMAV_UNAVAILABLE";
    public static final String CONTENT_UNAVAILABLE = "CONTENT_UNAVAILABLE";
    public static final String DECLARED_SIZE_MISMATCH = "DECLARED_SIZE_MISMATCH";
    public static final String FILE_NOT_AVAILABLE = "FILE_NOT_AVAILABLE";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String INCOMPLETE_STREAM = "INCOMPLETE_STREAM";
    public static final String INVALID_CONTENT_TYPE = "INVALID_CONTENT_TYPE";
    public static final String INVALID_FIELD_PREFIX = "INVALID_";
    public static final String INVALID_FILENAME = "INVALID_FILENAME";
    public static final String MAX_SIZE_EXCEEDED = "MAX_SIZE_EXCEEDED";
    public static final String SCAN_ATTEMPTS_EXHAUSTED = "SCAN_ATTEMPTS_EXHAUSTED";
    public static final String SCAN_LEASE_EXPIRED = "SCAN_LEASE_EXPIRED";
    public static final String SCAN_MESSAGE_MISMATCH = "SCAN_MESSAGE_MISMATCH";
    public static final String STORAGE_CONTENT_SHA256_MISMATCH = "STORAGE_CONTENT_SHA256_MISMATCH";
    public static final String STORAGE_CONTENT_SIZE_MISMATCH = "STORAGE_CONTENT_SIZE_MISMATCH";
    public static final String STORAGE_INTEGRITY_MISMATCH = "STORAGE_INTEGRITY_MISMATCH";
    public static final String STORAGE_OBJECT_NOT_FOUND = "STORAGE_OBJECT_NOT_FOUND";
    public static final String STORAGE_SIZE_MISMATCH = "STORAGE_SIZE_MISMATCH";
    public static final String STORAGE_VERSION_MISMATCH = "STORAGE_VERSION_MISMATCH";
    public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    public static final String UPLOAD_READ_FAILED = "UPLOAD_READ_FAILED";

    private FileFailureCodes() {
    }
}