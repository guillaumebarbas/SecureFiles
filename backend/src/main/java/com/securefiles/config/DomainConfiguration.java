package com.securefiles.config;

import com.securefiles.application.mapper.UploadFileMapper;
import com.securefiles.application.mapper.UploadConfigurationMapper;
import com.securefiles.application.mapper.FileMetadataMapper;
import com.securefiles.domain.file.port.in.GetFileMetadata;
import com.securefiles.domain.file.port.in.GetUploadConfiguration;
import com.securefiles.domain.file.port.in.DownloadFile;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.ScanFile;
import com.securefiles.domain.file.port.in.UploadFile;
import com.securefiles.domain.file.port.out.AntivirusScanner;
import com.securefiles.domain.file.port.out.FileAcceptancePort;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.domain.file.usecases.DownloadFileUseCase;
import com.securefiles.domain.file.usecases.GetFileMetadataUseCase;
import com.securefiles.domain.file.usecases.GetUploadConfigurationUseCase;
import com.securefiles.domain.file.usecases.ListFilesUseCase;
import com.securefiles.domain.file.usecases.ScanFileUseCase;
import com.securefiles.domain.file.usecases.UploadFileUseCase;
import com.securefiles.infrastructure.mapper.OutboxEventMapper;
import com.securefiles.infrastructure.mapper.ScanAttemptEntityMapper;
import com.securefiles.infrastructure.mapper.StoredFileEntityMapper;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ScanProperties.class, UploadProperties.class})
public class DomainConfiguration {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneOffset.UTC);
    }

    @Bean
    public UploadFileMapper uploadFileMapper() {
        return new UploadFileMapper();
    }

    @Bean
    public UploadConfigurationMapper uploadConfigurationMapper() {
        return new UploadConfigurationMapper();
    }

    @Bean
    public FileMetadataMapper fileMetadataMapper() {
        return new FileMetadataMapper();
    }

    @Bean
    public StoredFileEntityMapper storedFileEntityMapper() {
        return new StoredFileEntityMapper();
    }

    @Bean
    public ScanAttemptEntityMapper scanAttemptEntityMapper() {
        return new ScanAttemptEntityMapper();
    }

    @Bean
    public OutboxEventMapper outboxEventMapper() {
        return new OutboxEventMapper();
    }

    @Bean
    public UploadFile uploadFile(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            FileAcceptancePort acceptancePort,
            Clock applicationClock,
            UploadProperties properties) {
        return new UploadFileUseCase(
                repository,
                contentStorage,
                acceptancePort,
                applicationClock,
                UUID::randomUUID,
                properties.maximumSize().toBytes());
    }

    @Bean
    public GetUploadConfiguration getUploadConfiguration(UploadProperties properties) {
        return new GetUploadConfigurationUseCase(properties.maximumSize().toBytes());
    }

    @Bean
    public ScanFile scanFile(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            AntivirusScanner antivirusScanner,
            Clock applicationClock,
            ScanProperties properties) {
        return new ScanFileUseCase(
                repository,
                contentStorage,
                antivirusScanner,
                applicationClock,
                UUID::randomUUID,
                properties.leaseDuration(),
                properties.maximumAttempts(),
                properties.retryDelay());
    }

    @Bean
    public DownloadFile downloadFile(
            StoredFileRepository repository,
            FileContentStorage contentStorage) {
        return new DownloadFileUseCase(repository, contentStorage);
    }

    @Bean
    public GetFileMetadata getFileMetadata(StoredFileRepository repository) {
        return new GetFileMetadataUseCase(repository);
    }

    @Bean
    public ListFiles listFiles(StoredFileRepository repository) {
        return new ListFilesUseCase(repository);
    }
}
