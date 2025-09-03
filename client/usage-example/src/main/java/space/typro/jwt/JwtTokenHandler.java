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

    
}