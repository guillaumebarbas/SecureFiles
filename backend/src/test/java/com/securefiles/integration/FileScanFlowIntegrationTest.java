package com.securefiles.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.awaitility.core.ConditionTimeoutException;

@EnabledIfEnvironmentVariable(named = "SECUREFILES_INTEGRATION", matches = "true")
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "securefiles.rabbitmq.exchange=securefiles.integration.scan",
    "securefiles.rabbitmq.queue=securefiles.integration.scan",
    "securefiles.rabbitmq.routing-key=file.scan.integration",
    "securefiles.rabbitmq.retry-queue=securefiles.integration.scan.retry",
    "securefiles.rabbitmq.dead-letter-queue=securefiles.integration.scan.dlq",
    "securefiles.rabbitmq.relay-interval-millis=100",
    "securefiles.rabbitmq.maximum-dead-letter-redrives=1",
    "securefiles.scan.lease-duration=PT30S",
    "securefiles.scan.retry-delay=PT1S",
    "securefiles.quota.per-owner=10737418240B"
})
class FileScanFlowIntegrationTest {

    private static final long LARGE_FILE_SIZE_BYTES = 19_553_061L;
    private static final int STREAM_BLOCK_SIZE_BYTES = 8 * 1024;
    private static final String INTEGRATION_PASSWORD = "integration-password";
    private static final String EICAR_TEXT_PATH_ENVIRONMENT_VARIABLE = "SECUREFILES_EICAR_TEXT_PATH";
    private static final String EICAR_ZIP_PATH_ENVIRONMENT_VARIABLE = "SECUREFILES_EICAR_ZIP_PATH";
    private static final String FILE_ID_HEADER = "securefiles-file-id";
    private static final String REDRIVE_COUNT_HEADER = "securefiles-redrive-count";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @LocalServerPort
    private int serverPort;

    @Test
    void upload_shouldReachClean_whenScanInfrastructureIsAvailable() {
        String authenticationCookie = authenticationCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.COOKIE, authenticationCookie);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("file", new ClassPathResource("integration/safe-file.txt"));

        ResponseEntity<JsonNode> uploadHttpResponse = restTemplate.postForEntity(
                apiUrl(),
                new HttpEntity<>(requestBody, headers),
                JsonNode.class);

        assertThat(uploadHttpResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode uploadResponse = uploadHttpResponse.getBody();
        assertThat(uploadResponse).isNotNull();
        assertThat(uploadResponse.path("status").asText()).isEqualTo("PENDING_SCAN");
        UUID fileId = UUID.fromString(uploadResponse.path("fileId").asText());

        JsonNode terminalResponse;
        try {
            terminalResponse = await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .until(
                    () -> restTemplate.exchange(
                                    apiUrl() + "/" + fileId,
                                    org.springframework.http.HttpMethod.GET,
                                    authenticatedEntity(authenticationCookie),
                                    JsonNode.class)
                            .getBody(),
                    response -> isTerminal(response.path("status").asText()));
            } catch (ConditionTimeoutException exception) {
                throw new AssertionError(buildTerminalDiagnostic(fileId), exception);
            }

            assertThat(terminalResponse.path("status").asText()).isEqualTo("CLEAN");
        }

    @Test
    void upload_shouldIncreaseUsedQuota_whenScanReachesClean() throws Exception {
        String authenticationCookie = authenticationCookie();
        ResponseEntity<JsonNode> initialQuotaResponse = restTemplate.exchange(
                storageQuotaUrl(),
                org.springframework.http.HttpMethod.GET,
                authenticatedEntity(authenticationCookie),
                JsonNode.class);

        assertThat(initialQuotaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode initialQuota = initialQuotaResponse.getBody();
        assertThat(initialQuota).isNotNull();
        long initialUsedBytes = initialQuota.path("usedBytes").asLong();
        long initialQuotaBytes = initialQuota.path("quotaBytes").asLong();
        assertThat(initialUsedBytes).isZero();
        assertThat(initialQuotaBytes).isEqualTo(10_737_418_240L);

        ClassPathResource file = new ClassPathResource("integration/safe-file.txt");
        long uploadedSizeBytes = file.contentLength();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.COOKIE, authenticationCookie);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("file", file);

        ResponseEntity<JsonNode> uploadHttpResponse = restTemplate.postForEntity(
                apiUrl(),
                new HttpEntity<>(requestBody, headers),
                JsonNode.class);

        assertThat(uploadHttpResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(uploadHttpResponse.getBody()).isNotNull();
        assertThat(uploadHttpResponse.getBody().path("status").asText()).isEqualTo("PENDING_SCAN");
        UUID fileId = UUID.fromString(uploadHttpResponse.getBody().path("fileId").asText());

        JsonNode terminalResponse;
        try {
            terminalResponse = await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .until(
                    () -> restTemplate.exchange(
                            apiUrl() + "/" + fileId,
                            org.springframework.http.HttpMethod.GET,
                            authenticatedEntity(authenticationCookie),
                            JsonNode.class)
                        .getBody(),
                    response -> isTerminal(response.path("status").asText()));
        } catch (ConditionTimeoutException exception) {
            throw new AssertionError(buildTerminalDiagnostic(fileId), exception);
        }

        assertThat(terminalResponse.path("status").asText()).isEqualTo("CLEAN");

        ResponseEntity<JsonNode> updatedQuotaResponse = restTemplate.exchange(
                storageQuotaUrl(),
                org.springframework.http.HttpMethod.GET,
                authenticatedEntity(authenticationCookie),
                JsonNode.class);

        assertThat(updatedQuotaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updatedQuota = updatedQuotaResponse.getBody();
        assertThat(updatedQuota).isNotNull();
        long updatedUsedBytes = updatedQuota.path("usedBytes").asLong();
        long updatedQuotaBytes = updatedQuota.path("quotaBytes").asLong();
        long updatedAvailableBytes = updatedQuotaBytes - updatedUsedBytes;

        assertThat(updatedQuotaBytes).isEqualTo(initialQuotaBytes);
        assertThat(updatedUsedBytes).isEqualTo(initialUsedBytes + uploadedSizeBytes);
        assertThat(updatedAvailableBytes).isLessThan(initialQuotaBytes - initialUsedBytes);
    }

    @Test
    void upload_shouldReachClean_whenMultipartStorageIsUsed() throws Exception {
        String authenticationCookie = authenticationCookie();
        Path largeFile = Files.createTempFile("securefiles-large-upload-", ".mov");
        try {
            byte[] streamingBlock = new byte[STREAM_BLOCK_SIZE_BYTES];
            long remainingBytes = LARGE_FILE_SIZE_BYTES;
            try (OutputStream output = Files.newOutputStream(largeFile)) {
                while (remainingBytes > 0) {
                    int bytesToWrite = (int) Math.min(streamingBlock.length, remainingBytes);
                    output.write(streamingBlock, 0, bytesToWrite);
                    remainingBytes -= bytesToWrite;
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set(HttpHeaders.COOKIE, authenticationCookie);
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("file", new FileSystemResource(largeFile.toFile()));

            ResponseEntity<JsonNode> uploadHttpResponse = restTemplate.postForEntity(
                    apiUrl(),
                    new HttpEntity<>(requestBody, headers),
                    JsonNode.class);

            JsonNode uploadResponse = uploadHttpResponse.getBody();
                assertThat(uploadHttpResponse.getStatusCode())
                    .withFailMessage("Large multipart upload was rejected: %s", uploadResponse)
                    .isEqualTo(HttpStatus.ACCEPTED);
            assertThat(uploadResponse).isNotNull();
            assertThat(uploadResponse.path("sizeBytes").asLong()).isEqualTo(LARGE_FILE_SIZE_BYTES);
            UUID fileId = UUID.fromString(uploadResponse.path("fileId").asText());

            JsonNode terminalResponse;
            try {
                terminalResponse = await()
                        .atMost(Duration.ofSeconds(60))
                        .pollInterval(Duration.ofMillis(250))
                        .until(
                                () -> restTemplate.exchange(
                                        apiUrl() + "/" + fileId,
                                        org.springframework.http.HttpMethod.GET,
                                        authenticatedEntity(authenticationCookie),
                                        JsonNode.class)
                                    .getBody(),
                                response -> isTerminal(response.path("status").asText()));
            } catch (ConditionTimeoutException exception) {
                throw new AssertionError(buildTerminalDiagnostic(fileId), exception);
            }

            assertThat(terminalResponse.path("status").asText()).isEqualTo("CLEAN");
        } finally {
            Files.deleteIfExists(largeFile);
        }
    }

    @Test
    void upload_shouldReachInfected_whenEicarTextFixtureIsConfigured() {
        assertUploadedFileReachesInfected(externalTestFile(EICAR_TEXT_PATH_ENVIRONMENT_VARIABLE));
    }

    @Test
    void upload_shouldReachInfected_whenEicarZipFixtureIsConfigured() {
        assertUploadedFileReachesInfected(externalTestFile(EICAR_ZIP_PATH_ENVIRONMENT_VARIABLE));
    }

    @Test
    void deadLetter_shouldFailPendingScan_whenCorrelatedMessageCannotBeDecoded() {
        UUID fileId = UUID.randomUUID();
        createPendingScanFile(fileId);
        try {
            rabbitTemplate.send(
                    "securefiles.integration.scan",
                    "securefiles.integration.scan.dlq",
                    malformedDeadLetterMessage(fileId));

            String status = await()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofMillis(250))
                    .until(() -> fileStatus(fileId), "SCAN_FAILED"::equals);

            assertThat(status).isEqualTo("SCAN_FAILED");
            assertThat(querySingleRow(
                    "select failure_code from stored_file where id = ?",
                    fileId).get("failure_code"))
                    .isEqualTo("SCAN_ATTEMPTS_EXHAUSTED");
        } finally {
            jdbcTemplate.update("delete from stored_file where id = ?", fileId);
        }
    }

    private void assertUploadedFileReachesInfected(Path file) {
        assertUploadedFileReachesStatus(file, "INFECTED", Duration.ofSeconds(30));
    }

    private void assertUploadedFileReachesStatus(Path file, String expectedStatus, Duration timeout) {
        String authenticationCookie = authenticationCookie();
        long initialUsedBytes = usedStorageBytes(authenticationCookie);
        long uploadedSizeBytes = file.toFile().length();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.COOKIE, authenticationCookie);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("file", new FileSystemResource(file.toFile()));

        ResponseEntity<JsonNode> uploadHttpResponse = restTemplate.postForEntity(
                apiUrl(),
                new HttpEntity<>(requestBody, headers),
                JsonNode.class);

        assertThat(uploadHttpResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode uploadResponse = uploadHttpResponse.getBody();
        assertThat(uploadResponse).isNotNull();
        assertThat(uploadResponse.path("status").asText()).isEqualTo("PENDING_SCAN");
        UUID fileId = UUID.fromString(uploadResponse.path("fileId").asText());

        JsonNode terminalResponse;
        try {
            terminalResponse = await()
                .atMost(timeout)
                    .pollInterval(Duration.ofMillis(250))
                    .until(
                                () -> restTemplate.exchange(
                                        apiUrl() + "/" + fileId,
                                        org.springframework.http.HttpMethod.GET,
                                        authenticatedEntity(authenticationCookie),
                                        JsonNode.class)
                                    .getBody(),
                            response -> isTerminal(response.path("status").asText()));
        } catch (ConditionTimeoutException exception) {
            throw new AssertionError(buildTerminalDiagnostic(fileId), exception);
        }

        String actualStatus = terminalResponse.path("status").asText();
        assertThat(actualStatus)
            .withFailMessage(
                "File %s reached status %s instead of %s. metadata=%s, %s",
                fileId,
                actualStatus,
                expectedStatus,
                terminalResponse,
                buildTerminalDiagnostic(fileId))
            .isEqualTo(expectedStatus);

            long expectedUsedBytes = "CLEAN".equals(expectedStatus)
                ? initialUsedBytes + uploadedSizeBytes
                : initialUsedBytes;
            assertThat(usedStorageBytes(authenticationCookie)).isEqualTo(expectedUsedBytes);
    }

    private Path externalTestFile(String environmentVariable) {
        String configuredPath = System.getenv(environmentVariable);
        Assumptions.assumeTrue(
                configuredPath != null && !configuredPath.isBlank(),
            () -> "Set " + environmentVariable + " to execute this integration test.");
        Path file = Path.of(configuredPath);
        assertThat(Files.isRegularFile(file))
                .as("The file configured through %s must be a regular file", environmentVariable)
                .isTrue();
        assertThat(Files.isReadable(file))
                .as("The file configured through %s must be readable", environmentVariable)
                .isTrue();
        return file;
    }


    private Message malformedDeadLetterMessage(UUID fileId) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MediaType.APPLICATION_JSON_VALUE);
        properties.setHeader(FILE_ID_HEADER, fileId.toString());
        properties.setHeader(REDRIVE_COUNT_HEADER, 1);
        return new Message("not-a-scan-message".getBytes(StandardCharsets.UTF_8), properties);
    }

    private void createPendingScanFile(UUID fileId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                insert into stored_file (
                    id, owner_id, original_filename, client_content_type, status,
                    scan_attempt_count, created_at, updated_at, entity_version)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                new Object[] {
                    fileId,
                    "dead-letter-integration-owner",
                    "malformed-message.bin",
                    "application/octet-stream",
                    "PENDING_SCAN",
                    0,
                    now,
                    now,
                    0L
                },
                new int[] {
                    Types.OTHER,
                    Types.VARCHAR,
                    Types.VARCHAR,
                    Types.VARCHAR,
                    Types.VARCHAR,
                    Types.INTEGER,
                    Types.TIMESTAMP_WITH_TIMEZONE,
                    Types.TIMESTAMP_WITH_TIMEZONE,
                    Types.BIGINT
                });
    }

    private String fileStatus(UUID fileId) {
        return jdbcTemplate.queryForObject(
                "select status from stored_file where id = ?",
                String.class,
                fileId);
    }
    private String buildTerminalDiagnostic(UUID fileId) {
        Map<String, Object> file = querySingleRow(
            "select status, scan_attempt_count, scan_lease_until, next_scan_at, "
                + "failure_code, updated_at from stored_file where id = ?",
            fileId);
        List<Map<String, Object>> attempts = jdbcTemplate.queryForList(
            "select attempt_number, verdict, failure_code from scan_attempt where file_id = ? "
                + "order by attempt_number",
            fileId);
        Map<String, Object> outbox = querySingleRow(
            "select published_at, publish_attempts, event_type from outbox_event where file_id = ?",
            fileId);
        return "stored_file=" + file
            + ", scan_attempt=" + attempts
            + ", outbox_event=" + outbox;
    }

    private Map<String, Object> querySingleRow(String sql, UUID fileId) {
        try {
            return jdbcTemplate.queryForMap(sql, fileId);
        } catch (EmptyResultDataAccessException exception) {
            return Map.of();
        }
    }

    private boolean isTerminal(String status) {
        return status.equals("CLEAN")
                || status.equals("INFECTED")
                || status.equals("SCAN_FAILED");
    }

    private String apiUrl() {
        return "http://localhost:" + serverPort + "/api/v1/files";
    }

    private String storageQuotaUrl() {
        return "http://localhost:" + serverPort + "/api/v1/users/me/storage";
    }

    private long usedStorageBytes(String authenticationCookie) {
        ResponseEntity<JsonNode> quotaResponse = restTemplate.exchange(
                storageQuotaUrl(),
                org.springframework.http.HttpMethod.GET,
                authenticatedEntity(authenticationCookie),
                JsonNode.class);
        assertThat(quotaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode quota = quotaResponse.getBody();
        assertThat(quota).isNotNull();
        return quota.path("usedBytes").asLong();
    }

    private String authenticationCookie() {
        String userName = "scan-integration-" + UUID.randomUUID();
        ResponseEntity<JsonNode> registrationResponse = restTemplate.postForEntity(
                "http://localhost:" + serverPort + "/api/v1/auth/register",
                jsonEntity(Map.of(
                        "name", userName,
                        "password", INTEGRATION_PASSWORD,
                        "roles", List.of("utilisateur"))),
                JsonNode.class);
        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<JsonNode> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + serverPort + "/api/v1/auth/login",
                jsonEntity(Map.of("name", userName, "password", INTEGRATION_PASSWORD)),
                JsonNode.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return cookiePair(loginResponse);
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authenticatedEntity(String authenticationCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, authenticationCookie);
        return new HttpEntity<>(headers);
    }

    private String cookiePair(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }
}