package com.securefiles.infrastructure.minio;

import com.securefiles.domain.file.model.StorageMetadata;
import com.securefiles.domain.file.model.StorageObjectNotFoundException;
import com.securefiles.domain.file.model.StorageReceipt;
import com.securefiles.domain.file.port.out.FileContentStorage;
import io.minio.errors.ErrorResponseException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

public final class MinioFileContentStorage implements FileContentStorage {

    private static final long UNKNOWN_OBJECT_SIZE = -1L;
    private static final long PART_SIZE_BYTES = 10 * 1024 * 1024L;

    private final MinioClient minioClient;
    private final String bucket;
    private final String quarantinePrefix;

    public MinioFileContentStorage(
            MinioClient minioClient,
            String bucket,
            String quarantinePrefix) {
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient must not be null");
        this.bucket = requireText(bucket, "bucket");
        this.quarantinePrefix = normalizePrefix(quarantinePrefix);
    }

    @Override
    public StorageReceipt store(UUID fileId, InputStream content) {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey(fileId))
                    .stream(content, UNKNOWN_OBJECT_SIZE, PART_SIZE_BYTES)
                    .contentType("application/octet-stream")
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("The file content could not be stored", exception);
        }
        StorageMetadata storageMetadata = head(fileId);
        return new StorageReceipt(objectKey(fileId), storageMetadata.storageVersion());
    }

    @Override
    public StorageMetadata head(UUID fileId) {
        Objects.requireNonNull(fileId, "fileId must not be null");
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey(fileId))
                    .build());
            return new StorageMetadata(response.size(), storageVersion(response.versionId(), response.etag()));
        } catch (ErrorResponseException exception) {
            if (isObjectMissing(exception)) {
                throw new StorageObjectNotFoundException();
            }
            throw new IllegalStateException("The file content metadata could not be read", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("The file content metadata could not be read", exception);
        }
    }

    @Override
    public InputStream openStream(UUID fileId) {
        Objects.requireNonNull(fileId, "fileId must not be null");
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey(fileId))
                    .build());
        } catch (ErrorResponseException exception) {
            if (isObjectMissing(exception)) {
                throw new StorageObjectNotFoundException();
            }
            throw new IllegalStateException("The file content could not be opened", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("The file content could not be opened", exception);
        }
    }

    @Override
    public void delete(UUID fileId) {
        Objects.requireNonNull(fileId, "fileId must not be null");
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey(fileId))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("The file content could not be deleted", exception);
        }
    }

    private String objectKey(UUID fileId) {
        return quarantinePrefix + "/" + fileId + "/payload";
    }

    private String storageVersion(String versionId, String etag) {
        if (versionId != null && !versionId.isBlank()) {
            return versionId;
        }
        if (etag != null && !etag.isBlank()) {
            return etag;
        }
        throw new IllegalStateException("The object has no storage version");
    }

    private boolean isObjectMissing(ErrorResponseException exception) {
        return "NoSuchKey".equals(exception.errorResponse().code());
    }

    private String normalizePrefix(String prefix) {
        String normalizedPrefix = requireText(prefix, "quarantinePrefix").replaceAll("^/+|/+$", "");
        if (normalizedPrefix.contains("..") || normalizedPrefix.contains("\\")) {
            throw new IllegalArgumentException("quarantinePrefix contains an unsafe path");
        }
        return normalizedPrefix;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
