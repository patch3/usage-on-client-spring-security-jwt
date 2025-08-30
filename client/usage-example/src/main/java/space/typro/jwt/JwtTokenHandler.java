package space.typro.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import space.typro.service.HttpClientService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Slf4j
public class JwtTokenHandler {
    private final ObjectMapper objectMapper;

    public JwtTokenHandler() { // Конструктор без ключей
        this.objectMapper = new ObjectMapper();
    }

    public Optional<Tokens> extractTokensFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String accessToken = jsonNode.path("access_token").asText(null);
            String refreshToken = jsonNode.path("refresh_token").asText(null); // Might be null

            if (accessToken != null && !accessToken.isEmpty()) {
                return Optional.of(new Tokens(accessToken, refreshToken));
            } else {
                log.warn("Access token not found in response body.");
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error parsing tokens from response body", e);
            return Optional.empty();
        }
    }


    /**
     * Refreshes the access token by calling the server's refresh endpoint.
     * Assumes the endpoint expects the refresh token in the Authorization: Bearer header
     * and returns a new access token (and optionally a new refresh token) in the response body.
     */
    public Optional<String> refreshAccessToken(HttpClientService httpClientService, String refreshEndpointUrl, String refreshTokenString) {
        if (refreshTokenString == null || refreshTokenString.isEmpty()) {
            log.warn("No refresh token provided for refresh.");
            return Optional.empty();
        }

        try {
            URI refreshUri = new URI(refreshEndpointUrl);
            // The server might expect the refresh token in the body or header.
            // Based on common patterns and your server setup, let's try header first.
            // If that fails, you might need to adjust this part.
            Optional<String> refreshResponse = httpClientService.postWithBearerToken(refreshUri, "{}", refreshTokenString); // Empty JSON body as placeholder

            if (refreshResponse.isPresent()) {
                // Parse the response to get the new access token
                return extractTokensFromResponse(refreshResponse.get()).map(Tokens::accessToken);
            } else {
                log.warn("Refresh token request failed or returned empty response.");
                return Optional.empty();
            }
        } catch (URISyntaxException e) {
            log.error("Invalid refresh endpoint URI: {}", refreshEndpointUrl, e);
            return Optional.empty();
        }
    }
}