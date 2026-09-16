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
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@EnabledIfEnvironmentVariable(named = "SECUREFILES_SCAN_LIMIT_INTEGRATION", matches = "true")
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "securefiles.rabbitmq.exchange=securefiles.integration.scan-limit",
    "securefiles.rabbitmq.queue=securefiles.integration.scan-limit",
    "securefiles.rabbitmq.routing-key=file.scan.integration.scan-limit",
    "securefiles.rabbitmq.retry-queue=securefiles.integration.scan-limit.retry",
    "securefiles.rabbitmq.dead-letter-queue=securefiles.integration.scan-limit.dlq",
    "securefiles.rabbitmq.relay-interval-millis=100",
    "securefiles.scan.lease-duration=PT30S",
    "securefiles.scan.maximum-attempts=1",
    "securefiles.scan.retry-delay=PT1S"
})
class FileScanSizeLimitIntegrationTest {

    private static final long BELOW_TEST_LIMIT_SIZE_BYTES = 512 * 1024L;
    private static final long ABOVE_TEST_LIMIT_SIZE_BYTES = 2 * 1024 * 1024L;
    private static final int STREAM_BLOCK_SIZE_BYTES = 8 * 1024;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int serverPort;

    @Test
    void upload_shouldReachClean_whenGeneratedFileIsBelowTestOnlyClamAvLimit() throws Exception {
        Path file = createGeneratedFile("securefiles-below-clamav-limit-", BELOW_TEST_LIMIT_SIZE_BYTES);
        try {
            assertUploadedFileReachesStatus(file, "CLEAN", Duration.ofSeconds(30));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void upload_shouldFailClosed_whenGeneratedFileExceedsTestOnlyClamAvLimit() throws Exception {
        Path file = createGeneratedFile("securefiles-above-clamav-limit-", ABOVE_TEST_LIMIT_SIZE_BYTES);
        try {
            UUID fileId = uploadFile(file);
            JsonNode terminalResponse = awaitTerminalStatus(fileId, Duration.ofSeconds(30));

            assertThat(terminalResponse.path("status").asText())
                    .withFailMessage("Unexpected scan result: %s. %s", terminalResponse, buildDiagnostic(fileId))
                    .isEqualTo("SCAN_FAILED");

                ResponseEntity<Void> downloadResponse = restTemplate.getForEntity(
                    apiUrl() + "/" + fileId + "/content",
                    Void.class);
            assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void assertUploadedFileReachesStatus(Path file, String expectedStatus, Duration timeout) {
        UUID fileId = uploadFile(file);
        JsonNode terminalResponse = awaitTerminalStatus(fileId, timeout);

        assertThat(terminalResponse.path("status").asText())
                .withFailMessage("Unexpected scan result: %s. %s", terminalResponse, buildDiagnostic(fileId))
                .isEqualTo(expectedStatus);
    }

    private UUID uploadFile(Path file) {
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
        return UUID.fromString(uploadResponse.path("fileId").asText());
    }

    private JsonNode awaitTerminalStatus(UUID fileId, Duration timeout) {
        try {
            return await()
                    .atMost(timeout)
                    .pollInterval(Duration.ofMillis(250))
                    .until(
                            () -> restTemplate.getForObject(apiUrl() + "/" + fileId, JsonNode.class),
                            response -> isTerminal(response.path("status").asText()));
        } catch (ConditionTimeoutException exception) {
            throw new AssertionError(buildDiagnostic(fileId), exception);
        }
    }

    private Path createGeneratedFile(String prefix, long sizeBytes) throws Exception {
        Path file = Files.createTempFile(prefix, ".bin");
        byte[] block = new byte[STREAM_BLOCK_SIZE_BYTES];
        long remainingBytes = sizeBytes;
        try (OutputStream output = Files.newOutputStream(file)) {
            while (remainingBytes > 0) {
                int bytesToWrite = (int) Math.min(block.length, remainingBytes);
                output.write(block, 0, bytesToWrite);
                remainingBytes -= bytesToWrite;
            }
        }
        return file;
    }

    private String buildDiagnostic(UUID fileId) {
        Map<String, Object> file = querySingleRow(
                "select status, scan_attempt_count, scan_lease_until, next_scan_at, "
                        + "failure_code, updated_at from stored_file where id = ?",
                fileId);
        List<Map<String, Object>> attempts = jdbcTemplate.queryForList(
                "select attempt_number, verdict, failure_code from scan_attempt where file_id = ? "
                        + "order by attempt_number",
                fileId);
        return "stored_file=" + file + ", scan_attempt=" + attempts;
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