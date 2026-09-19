package com.securefiles.config;

import com.securefiles.application.mapper.UploadFileMapper;
import com.securefiles.application.mapper.UploadConfigurationMapper;
import com.securefiles.application.mapper.FileMetadataMapper;
import com.securefiles.application.mapper.UserMapper;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.AuthenticateUser;
import com.securefiles.domain.user.port.in.CreateUser;
import com.securefiles.domain.file.port.in.GetFileMetadata;
import com.securefiles.domain.file.port.in.GetUploadConfiguration;
import com.securefiles.domain.file.port.in.DownloadFile;
import com.securefiles.domain.file.port.in.DeleteFile;
import com.securefiles.domain.file.port.in.FailScan;
import com.securefiles.domain.file.port.in.ListFiles;
import com.securefiles.domain.file.port.in.RecoverExpiredScan;
import com.securefiles.domain.file.port.in.ScanFile;
import com.securefiles.domain.file.port.in.UploadFile;
import com.securefiles.domain.file.port.out.AntivirusScanner;
import com.securefiles.domain.file.port.out.ExpiredScanRecoveryPort;
import com.securefiles.domain.file.port.out.FileAcceptancePort;
import com.securefiles.domain.file.port.out.FileContentStorage;
import com.securefiles.domain.file.port.out.StoredFileRepository;
import com.securefiles.domain.file.usecases.DownloadFileUseCase;
import com.securefiles.domain.file.usecases.DeleteFileUseCase;
import com.securefiles.domain.file.usecases.FailScanUseCase;
import com.securefiles.domain.file.usecases.GetFileMetadataUseCase;
import com.securefiles.domain.file.usecases.GetUploadConfigurationUseCase;
import com.securefiles.domain.file.usecases.ListFilesUseCase;
import com.securefiles.domain.file.usecases.RecoverExpiredScanUseCase;
import com.securefiles.domain.file.usecases.ScanFileUseCase;
import com.securefiles.domain.file.usecases.UploadFileUseCase;
import com.securefiles.domain.user.port.in.GetCurrentUser;
import com.securefiles.domain.user.port.in.LogoutUser;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import com.securefiles.domain.user.port.out.AuthenticationTokenIssuer;
import com.securefiles.domain.user.port.out.PasswordHasher;
import com.securefiles.domain.user.port.out.UserRepository;
import com.securefiles.domain.user.usecases.AuthenticateUserUseCase;
import com.securefiles.domain.user.usecases.CreateUserUseCase;
import com.securefiles.domain.user.usecases.GetCurrentUserUseCase;
import com.securefiles.domain.user.usecases.LogoutUserUseCase;
import com.securefiles.infrastructure.mapper.OutboxEventMapper;
import com.securefiles.infrastructure.mapper.AuthenticationSessionEntityMapper;
import com.securefiles.infrastructure.mapper.ScanAttemptEntityMapper;
import com.securefiles.infrastructure.mapper.StoredFileEntityMapper;
import com.securefiles.infrastructure.mapper.UserEntityMapper;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    ScanProperties.class,
    UploadProperties.class,
    AuthenticationProperties.class,
    StorageMaintenanceProperties.class,
    QuotaProperties.class,
    RateLimitProperties.class
})
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
    public UserMapper userMapper() {
        return new UserMapper();
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
    public UserEntityMapper userEntityMapper() {
        return new UserEntityMapper();
    }

    @Bean
    public AuthenticationSessionEntityMapper authenticationSessionEntityMapper() {
        return new AuthenticationSessionEntityMapper();
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
    public RecoverExpiredScan recoverExpiredScan(
            ExpiredScanRecoveryPort recoveryPort,
            Clock applicationClock,
            ScanProperties properties) {
        return new RecoverExpiredScanUseCase(
                recoveryPort,
                applicationClock,
                UUID::randomUUID,
                properties.retryDelay());
    }

    @Bean
    public FailScan failScan(
            StoredFileRepository repository,
            Clock applicationClock) {
        return new FailScanUseCase(repository, applicationClock);
    }

    @Bean
    public DownloadFile downloadFile(
            StoredFileRepository repository,
            FileContentStorage contentStorage) {
        return new DownloadFileUseCase(repository, contentStorage);
    }

    @Bean
    public DeleteFile deleteFile(
            StoredFileRepository repository,
            FileContentStorage contentStorage,
            Clock applicationClock) {
        return new DeleteFileUseCase(repository, contentStorage, applicationClock);
    }

    @Bean
    public GetFileMetadata getFileMetadata(
            StoredFileRepository repository,
            UserRepository userRepository) {
        return new GetFileMetadataUseCase(repository, userRepository);
    }

    @Bean
    public ListFiles listFiles(
            StoredFileRepository repository,
            UserRepository userRepository) {
        return new ListFilesUseCase(repository, userRepository);
    }

    @Bean
    public CreateUser createUser(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            Clock applicationClock,
            AuthenticationProperties properties) {
        return new CreateUserUseCase(
                userRepository,
                passwordHasher,
                applicationClock,
                UUID::randomUUID,
                registrationRoles(properties));
    }

    @Bean
    public AuthenticateUser authenticateUser(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AuthenticationSessionRepository sessionRepository,
            AuthenticationTokenIssuer tokenIssuer,
            Clock applicationClock,
            AuthenticationProperties properties) {
        return new AuthenticateUserUseCase(
                userRepository,
                passwordHasher,
                sessionRepository,
                tokenIssuer,
                applicationClock,
                UUID::randomUUID,
                properties.tokenLifetime());
    }

    @Bean
    public GetCurrentUser getCurrentUser(UserRepository userRepository) {
        return new GetCurrentUserUseCase(userRepository);
    }

    @Bean
    public LogoutUser logoutUser(
            AuthenticationSessionRepository sessionRepository,
            Clock applicationClock) {
        return new LogoutUserUseCase(sessionRepository, applicationClock);
    }

    private Set<UserRole> registrationRoles(AuthenticationProperties properties) {
        return properties.registrationRoles().stream()
                .map(role -> parseRole(role))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown registration role", exception);
        }
    }
}
