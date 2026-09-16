package com.securefiles.domain.file.model.upload;

import com.securefiles.domain.file.model.FileFailureCodes;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class MeasuredInputStream extends FilterInputStream {

    private final MessageDigest digest;
    private final long maximumSizeBytes;
    private long measuredSizeBytes;
    private boolean reachedEnd;

    public MeasuredInputStream(InputStream inputStream) {
        this(inputStream, Long.MAX_VALUE);
    }

    public MeasuredInputStream(InputStream inputStream, long maximumSizeBytes) {
        super(inputStream);
        if (maximumSizeBytes < 0) {
            throw new IllegalArgumentException("maximumSizeBytes must not be negative");
        }
        this.maximumSizeBytes = maximumSizeBytes;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value == -1) {
            reachedEnd = true;
        } else {
            recordBytes(new byte[] {(byte) value}, 0, 1);
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int read = super.read(bytes, offset, length);
        if (read == -1) {
            reachedEnd = true;
        } else if (read > 0) {
            recordBytes(bytes, offset, read);
        }
        return read;
    }

    @Override
    public long skip(long numberOfBytes) throws IOException {
        throw new IOException("Skipping upload content is not supported");
    }

    public boolean hasReachedEnd() {
        return reachedEnd;
    }

    public long measuredSizeBytes() {
        return measuredSizeBytes;
    }

    public String sha256() {
        if (!reachedEnd) {
                throw new UploadException(
                    FileFailureCodes.INCOMPLETE_STREAM,
                    "The upload stream was not fully consumed.");
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void recordBytes(byte[] bytes, int offset, int length) {
        measuredSizeBytes += length;
        if (measuredSizeBytes > maximumSizeBytes) {
                throw new UploadException(
                    FileFailureCodes.MAX_SIZE_EXCEEDED,
                    "The file exceeds the maximum allowed size.");
        }
        digest.update(bytes, offset, length);
    }
}