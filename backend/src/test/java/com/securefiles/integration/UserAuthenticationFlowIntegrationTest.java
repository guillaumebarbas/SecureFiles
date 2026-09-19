package com.securefiles.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@EnabledIfEnvironmentVariable(named = "SECUREFILES_AUTH_INTEGRATION", matches = "true")
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserAuthenticationFlowIntegrationTest {

    private static final String API_ROOT = "/api/v1";
    private static final String PASSWORD = "integration-password";

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @LocalServerPort
        private int serverPort;

        @BeforeEach
        @AfterEach
        void clearLoginRateLimitBuckets() {
                jdbcTemplate.update("delete from api_rate_limit_bucket where bucket_key like 'login:ip:%'");
        }

        @Test
        void currentUser_shouldReturnNoContent_whenNoSessionCookieIsPresent() {
                ResponseEntity<Void> response = restTemplate.getForEntity(
                                apiUrl("/users/me"),
                                Void.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void files_shouldBePublic_whenNoSessionCookieIsPresent() {
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                                apiUrl("/files"),
                                JsonNode.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isObject()).isTrue();
                assertThat(response.getBody().path("content").isArray()).isTrue();
                assertThat(response.getBody().path("page").asInt()).isEqualTo(1);
        }

        @Test
        void uploadConfiguration_shouldBePublic_whenNoSessionCookieIsPresent() {
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                                apiUrl("/files/config"),
                                JsonNode.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().path("maximumSizeBytes").asLong()).isPositive();
        }

    @Test
    void authentication_shouldCreateLoadAndRevokeSession_whenUserUsesPublicFlow() {
        String userName = "auth-integration-" + UUID.randomUUID();
        Map<String, Object> registration = Map.of(
                "name", userName,
                "password", PASSWORD,
                "roles", List.of("developpeur", "utilisateur"));

        ResponseEntity<JsonNode> registrationResponse = restTemplate.postForEntity(
                apiUrl("/auth/register"),
                jsonEntity(registration),
                JsonNode.class);

        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registrationResponse.getBody()).isNotNull();
        assertThat(registrationResponse.getBody().path("name").asText()).isEqualTo(userName);
        assertThat(registrationResponse.getBody().path("roles"))
                .extracting(node -> node.asText())
                .containsExactly("developpeur", "utilisateur");

        ResponseEntity<JsonNode> loginResponse = restTemplate.postForEntity(
                apiUrl("/auth/login"),
                jsonEntity(Map.of("name", userName, "password", PASSWORD)),
                JsonNode.class);

        assertThat(loginResponse.getStatusCode())
                .as("Login response body: %s", loginResponse.getBody())
                .isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().has("accessToken")).isFalse();
        String authenticationCookie = cookiePair(loginResponse);

        ResponseEntity<JsonNode> currentUserResponse = restTemplate.exchange(
                apiUrl("/users/me"),
                HttpMethod.GET,
                authenticatedEntity(authenticationCookie),
                JsonNode.class);

        assertThat(currentUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentUserResponse.getBody()).isNotNull();
        assertThat(currentUserResponse.getBody().path("userId").asText())
                .isEqualTo(loginResponse.getBody().path("userId").asText());
        assertThat(currentUserResponse.getBody().path("roles"))
                .extracting(node -> node.asText())
                .containsExactly("developpeur", "utilisateur");

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                apiUrl("/auth/logout"),
                HttpMethod.POST,
                authenticatedEntity(authenticationCookie),
                Void.class);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logoutResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("Max-Age=0");

        ResponseEntity<JsonNode> revokedSessionResponse = restTemplate.exchange(
                apiUrl("/users/me"),
                HttpMethod.GET,
                authenticatedEntity(authenticationCookie),
                JsonNode.class);

        assertThat(revokedSessionResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

        @Test
        void authentication_shouldRejectDifferentSuffix_whenPasswordExceedsBcryptInputLimit() {
                String userName = "auth-long-password-" + UUID.randomUUID();
                String registeredPassword = "a".repeat(72) + "X";
                String differentPassword = "a".repeat(72) + "Y";
                Map<String, Object> registration = Map.of(
                                "name", userName,
                                "password", registeredPassword,
                                "roles", List.of("utilisateur"));

                ResponseEntity<JsonNode> registrationResponse = restTemplate.postForEntity(
                                apiUrl("/auth/register"),
                                jsonEntity(registration),
                                JsonNode.class);

                assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

                ResponseEntity<JsonNode> wrongPasswordResponse = restTemplate.postForEntity(
                                apiUrl("/auth/login"),
                                jsonEntity(Map.of("name", userName, "password", differentPassword)),
                                JsonNode.class);

                assertThat(wrongPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void login_shouldRejectTheSixthRequest_whenTheIpExceedsTheLoginRateLimit() {
                String userName = "auth-rate-limit-" + UUID.randomUUID();
                ResponseEntity<JsonNode> registrationResponse = restTemplate.postForEntity(
                                apiUrl("/auth/register"),
                                jsonEntity(Map.of(
                                                "name", userName,
                                                "password", PASSWORD,
                                                "roles", List.of("utilisateur"))),
                                JsonNode.class);
                assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

                for (int requestNumber = 0; requestNumber < 5; requestNumber++) {
                        ResponseEntity<JsonNode> loginResponse = restTemplate.postForEntity(
                                        apiUrl("/auth/login"),
                                        jsonEntity(Map.of("name", userName, "password", PASSWORD)),
                                        JsonNode.class);

                        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                }

                ResponseEntity<JsonNode> limitedResponse = restTemplate.postForEntity(
                                apiUrl("/auth/login"),
                                jsonEntity(Map.of("name", userName, "password", PASSWORD)),
                                JsonNode.class);

                assertThat(limitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(limitedResponse.getHeaders().getFirst("Retry-After")).isEqualTo("60");
                assertThat(limitedResponse.getBody()).isNotNull();
                assertThat(limitedResponse.getBody().path("code").asText())
                                .isEqualTo("LOGIN_RATE_LIMIT_EXCEEDED");
        }

    private String apiUrl(String path) {
        return "http://localhost:" + serverPort + API_ROOT + path;
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