package com.securefiles.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
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
    "securefiles.scan.lease-duration=PT30S",
    "securefiles.scan.retry-delay=PT1S"
})
class FileScanFlowIntegrationTest {

    private static final long LARGE_FILE_SIZE_BYTES = 19_553_061L;
    private static final int STREAM_BLOCK_SIZE_BYTES = 8 * 1024;
    private static final String EICAR_TEXT_PATH_ENVIRONMENT_VARIABLE = "SECUREFILES_EICAR_TEXT_PATH";
    private static final String EICAR_ZIP_PATH_ENVIRONMENT_VARIABLE = "SECUREFILES_EICAR_ZIP_PATH";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int serverPort;

    @Test
    void upload_shouldReachClean_whenScanInfrastructureIsAvailable() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
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
                    () -> restTemplate.getForObject(apiUrl() + "/" + fileId, JsonNode.class),
                    response -> isTerminal(response.path("status").asText()));
            } catch (ConditionTimeoutException exception) {
                throw new AssertionError(buildTimeoutDiagnostic(fileId), exception);
            }

            assertThat(terminalResponse.path("status").asText()).isEqualTo("CLEAN");
        }

    @Test
    void upload_shouldReachClean_whenMultipartStorageIsUsed() throws Exception {
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
                                () -> restTemplate.getForObject(apiUrl() + "/" + fileId, JsonNode.class),
                                response -> isTerminal(response.path("status").asText()));
            } catch (ConditionTimeoutException exception) {
                throw new AssertionError(buildTimeoutDiagnostic(fileId), exception);
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

    private void assertUploadedFileReachesInfected(Path file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
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
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofMillis(250))
                    .until(
                            () -> restTemplate.getForObject(apiUrl() + "/" + fileId, JsonNode.class),
                            response -> isTerminal(response.path("status").asText()));
        } catch (ConditionTimeoutException exception) {
            throw new AssertionError(buildTimeoutDiagnostic(fileId), exception);
        }

        assertThat(terminalResponse.path("status").asText()).isEqualTo("INFECTED");
    }

    private Path externalTestFile(String environmentVariable) {
        String configuredPath = System.getenv(environmentVariable);
        Assumptions.assumeTrue(
                configuredPath != null && !configuredPath.isBlank(),
                () -> "Set " + environmentVariable + " to execute this EICAR integration test.");
        Path file = Path.of(configuredPath);
        assertThat(Files.isRegularFile(file))
                .as("The file configured through %s must be a regular file", environmentVariable)
                .isTrue();
        assertThat(Files.isReadable(file))
                .as("The file configured through %s must be readable", environmentVariable)
                .isTrue();
        return file;
    }

    private String buildTimeoutDiagnostic(UUID fileId) {
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
        return "Scan did not reach a terminal status. stored_file=" + file
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
}