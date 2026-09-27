package com.harsh.shortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class AuthOwnershipIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String registerAndLogin(String email) throws Exception {
        Map<String, Object> registerBody = Map.of(
                "email", email,
                "password", "StrongPassword123"
        );

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerBody,
                String.class
        );

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        Map<String, Object> loginBody = Map.of(
                "email", email,
                "password", "StrongPassword123"
        );

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginBody,
                String.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        JsonNode responseJson = objectMapper.readTree(loginResponse.getBody());

        String token = responseJson.get("accessToken").asText();

        assertFalse(token.isBlank());

        return token;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createUrl(String token, String originalUrl) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "originalUrl", originalUrl
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                requestBody,
                authHeaders(token)
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        String shortCode = responseJson.get("shortCode").asText();

        assertFalse(shortCode.isBlank());

        return shortCode;
    }

    @Test
    void registrationAndLoginShouldWork() throws Exception {
        String email = uniqueEmail();

        Map<String, Object> registerBody = Map.of(
                "email", email,
                "password", "StrongPassword123"
        );

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerBody,
                String.class
        );

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        Map<String, Object> loginBody = Map.of(
                "email", email,
                "password", "StrongPassword123"
        );

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginBody,
                String.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        JsonNode json = objectMapper.readTree(loginResponse.getBody());

        assertTrue(json.hasNonNull("accessToken"));
        assertEquals("Bearer", json.get("tokenType").asText());
        assertTrue(json.get("expiresIn").asLong() > 0);
    }

    @Test
    void duplicateRegistrationShouldBeRejected() {
        String email = uniqueEmail();

        Map<String, Object> registerBody = Map.of(
                "email", email,
                "password", "StrongPassword123"
        );

        ResponseEntity<String> firstResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerBody,
                String.class
        );

        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

        ResponseEntity<String> secondResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerBody,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, secondResponse.getStatusCode());
    }

    @Test
    void protectedUrlCreationShouldRejectUnauthenticatedRequest() {
        Map<String, Object> requestBody = Map.of(
                "originalUrl", "https://example.com"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/urls",
                requestBody,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void userShouldBeAbleToCreateAndViewTheirOwnUrl() throws Exception {
        String token = registerAndLogin(uniqueEmail());

        String shortCode = createUrl(
                token,
                "https://example.com/my-page"
        );

        ResponseEntity<String> detailsResponse = restTemplate.exchange(
                "/api/v1/urls/" + shortCode,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );

        assertEquals(HttpStatus.OK, detailsResponse.getStatusCode());

        JsonNode details = objectMapper.readTree(detailsResponse.getBody());

        assertEquals(shortCode, details.get("shortCode").asText());
        assertEquals(
                "https://example.com/my-page",
                details.get("originalUrl").asText()
        );
        assertTrue(details.get("active").asBoolean());
    }

    @Test
    void userShouldNotBeAbleToViewAnotherUsersUrl() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail());
        String otherUserToken = registerAndLogin(uniqueEmail());

        String shortCode = createUrl(
                ownerToken,
                "https://example.com/private"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls/" + shortCode,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(otherUserToken)),
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void userShouldNotBeAbleToDeactivateAnotherUsersUrl() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail());
        String otherUserToken = registerAndLogin(uniqueEmail());

        String shortCode = createUrl(
                ownerToken,
                "https://example.com/private"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls/" + shortCode + "/deactivate",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(otherUserToken)),
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void userShouldOnlyReceiveTheirOwnUrlsInListing() throws Exception {
        String firstUserToken = registerAndLogin(uniqueEmail());
        String secondUserToken = registerAndLogin(uniqueEmail());

        String firstUserShortCode = createUrl(
                firstUserToken,
                "https://example.com/first-user"
        );

        createUrl(
                secondUserToken,
                "https://example.com/second-user"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(firstUserToken)),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        JsonNode content = json.get("content");

        assertTrue(content.isArray());
        assertTrue(content.size() >= 1);

        for (JsonNode url : content) {
            assertEquals(
                    firstUserShortCode,
                    url.get("shortCode").asText()
            );
        }
    }

    @Test
    void urlCreationShouldBeRateLimitedAfterTenRequests() throws Exception {
        String token = registerAndLogin(uniqueEmail());

        Map<String, Object> requestBody = Map.of(
                "originalUrl", "https://example.com/rate-limit-test"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                requestBody,
                authHeaders(token)
        );

        for (int i = 1; i <= 10; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/urls",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertEquals(
                    HttpStatus.CREATED,
                    response.getStatusCode(),
                    "Request " + i + " should be allowed"
            );
        }

        ResponseEntity<String> limitedResponse = restTemplate.exchange(
                "/api/v1/urls",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                limitedResponse.getStatusCode()
        );

        assertEquals(
                "60",
                limitedResponse.getHeaders().getFirst("Retry-After")
        );
    }
}