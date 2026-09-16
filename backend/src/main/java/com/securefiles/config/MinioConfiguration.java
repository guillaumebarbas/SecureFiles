package com.securefiles.config;

import com.securefiles.infrastructure.minio.MinioBucketInitializer;
import com.securefiles.infrastructure.minio.MinioFileContentStorage;
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public MinioFileContentStorage minioFileContentStorage(
            MinioClient minioClient,
            MinioProperties properties) {
        return new MinioFileContentStorage(
                minioClient,
                properties.bucket(),
                properties.quarantinePrefix());
    }

    @Bean
    public MinioBucketInitializer minioBucketInitializer(
            MinioClient minioClient,
            MinioProperties properties) {
        return new MinioBucketInitializer(minioClient, properties.bucket());
    }
}
