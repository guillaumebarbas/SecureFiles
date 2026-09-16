package com.securefiles.infrastructure.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import java.util.Objects;

public final class MinioBucketInitializer {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioBucketInitializer(MinioClient minioClient, String bucket) {
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient must not be null");
        this.bucket = Objects.requireNonNull(bucket, "bucket must not be null");
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("The MinIO bucket could not be initialized", exception);
        }
    }
}
