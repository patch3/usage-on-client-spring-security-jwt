package space.typro.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class HttpClientService {
    private final HttpClient httpClient;

    public HttpClientService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Performs a login request using HTTP Basic Authentication.
     * Assumes the server returns tokens in the response body upon successful login.
     * The request is typically a POST, often to /login.
     */
    public Optional<String> performBasicAuthLogin(URI loginUri, String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(loginUri)
                .header("Authorization", "Basic " + encodedCredentials)
                .header("Content-Type", "application/x-www-form-urlencoded") // Common for form-based login
                .POST(HttpRequest.BodyPublishers.ofString("")) // Empty body, credentials in header
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.debug("Login successful with status code 200");
                return Optional.of(response.body());
            } else {
                log.warn("Login failed with status code: {}. Response body: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Exception occurred during login request to {}", loginUri, e);
            return Optional.empty();
        }
    }

    /**
     * Performs a GET request with a Bearer token in the Authorization header.
     */
    public Optional<String> getWithBearerToken(URI uri, String token) {
        val request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.debug("GET request to {} successful", uri);
                return Optional.of(response.body());
            } else {
                log.warn("GET request to {} failed with status code: {}. Response body: {}", uri, response.statusCode(), response.body());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Exception occurred during GET request to {}", uri, e);
            return Optional.empty();
        }
    }

    /**
     * Performs a POST request with a Bearer token in the Authorization header.
     * The body is sent as-is (e.g., JSON string).
     */
    public Optional<String> postWithBearerToken(URI uri, String body, String token) {
        val request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json") // Assuming JSON payload
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("POST request to {} successful with status code {}", uri, response.statusCode());
                return Optional.of(response.body());
            } else {
                log.warn("POST request to {} failed with status code: {}. Response body: {}", uri, response.statusCode(), response.body());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Exception occurred during POST request to {}", uri, e);
            return Optional.empty();
        }
    }
}