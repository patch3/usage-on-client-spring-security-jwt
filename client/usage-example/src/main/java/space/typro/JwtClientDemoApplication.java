package space.typro;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import space.typro.config.ClientConfig;
import space.typro.config.ServerConfig;
import space.typro.jwt.JwtTokenHandler;
import space.typro.jwt.Tokens;
import space.typro.service.HttpClientService;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;

@Slf4j
public class JwtClientDemoApplication {
    private static final String SIMULATED_INPUT = "1\nj.jameson\npassword\n4";

    private final Scanner scanner;

    private final ClientConfig clientConfig;
    private final ServerConfig serverConfig;

    private final HttpClientService httpClientService;
    private final JwtTokenHandler jwtTokenHandler; // Без ключей

    private String currentAccessToken = null;
    private String currentRefreshToken = null;

    public JwtClientDemoApplication() {
        this.clientConfig = ClientConfig.getInstance();
        this.serverConfig = ServerConfig.getInstance();

        // Initialize HTTP Client
        val httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(clientConfig.httpTimeoutSeconds))
                .build();
        this.httpClientService = new HttpClientService(httpClient);
        // Initialize JWT Handler WITHOUT KEYS
        this.jwtTokenHandler = new JwtTokenHandler();

        if (clientConfig.interactive) {
            this.scanner = new Scanner(System.in);
        } else {
            System.out.println("Running in non-interactive mode");
            this.scanner = new Scanner(SIMULATED_INPUT);
        }
    }

    public static void main(String[] args) {
        try {
            JwtClientDemoApplication app = new JwtClientDemoApplication();
            app.run();
        } catch (Exception e) {
            log.error("Application failed to start", e);
            System.err.println("Critical error starting the application: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static final int MAX_REFRESH_ATTEMPTS = 3;
    private int refreshAttempts = 0;

    private void run() {
        while (true) {
            System.out.println("\n--- JWT Client Demo Menu ---");
            System.out.println("1. Login");
            if (currentAccessToken != null) {
                System.out.println("2. Access Protected Resource (using current Access Token)");
            }
            if (currentRefreshToken != null) {
                System.out.println("3. Refresh Access Token (using current Refresh Token)");
            }
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            String choice = this.scanner.nextLine();

            switch (choice) {
                case "1": // 1. Login
                    performLogin();
                    break;
                case "2": // 2. Access Protected Resource (using current Access Token)
                    if (isTokenValid(currentAccessToken)) {
                        accessProtectedResource();
                    } else {
                        System.out.println("You need to log in first or token is expired");
                        if (currentRefreshToken != null) {
                            System.out.println("Attempting to refresh token...");
                            refreshAccessToken();
                        }
                    }
                    break;
                case "3": // 3. Refresh Access Token (using current Refresh Token)
                    if (currentRefreshToken != null) {
                        refreshAccessToken();
                    } else {
                        System.out.println("You need to log in first to get a refresh token");
                        //TODO
                    }
                    break;
                case "4": // 4. Exit
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option. Please try again");
            }
        }
    }

    private void accessProtectedResource() {
        System.out.println("Accessing protected resource...");
        Optional<String> resourceResponse = httpClientService.getWithBearerToken(
                URI.create(serverConfig.getFullProtectedResourceUrl()), currentAccessToken);

        // Если запрос не удался, пробуем обновить токен
        if (resourceResponse.isEmpty() && currentRefreshToken != null) {
            System.out.println("Access token might be expired. Attempting to refresh...");
            refreshAccessToken();
            // Повторяем запрос после обновления токена
            if (currentAccessToken != null) {
                resourceResponse = httpClientService.getWithBearerToken(
                        URI.create(serverConfig.getFullProtectedResourceUrl()), currentAccessToken);
            }
        }

        resourceResponse.ifPresentOrElse(
                response -> {
                    System.out.println("Successfully accessed protected resource!");
                    System.out.println("Response: " + response);
                },
                () -> {
                    if (currentRefreshToken == null) {
                        System.out.println("Failed to access protected resource. No refresh token available.");
                    } else {
                        System.out.println("Failed to access protected resource even after token refresh.");
                    }
                }
        );
    }

    // Метод для очистки токенов
    private void clearTokens() {
        this.currentAccessToken = null;
        this.currentRefreshToken = null;
        this.refreshAttempts = 0;
        System.out.println("Tokens cleared. Please login again.");
    }

    private void refreshAccessToken() {
        System.out.println("Refreshing Access Token...");

        try {
            Optional<String> refreshResponse = httpClientService.postWithBearerToken(
                    URI.create(serverConfig.getFullRefreshUrl()), "{}", currentRefreshToken);

            if (refreshResponse.isPresent()) {
                Optional<Tokens> newTokensOpt = jwtTokenHandler.extractTokensFromResponse(refreshResponse.get());

                if (newTokensOpt.isPresent()) {
                    Tokens newTokens = newTokensOpt.get();
                    this.currentAccessToken = newTokens.accessToken();

                    // Обновляем refresh token, если сервер вернул новый
                    if (newTokens.refreshToken() != null) {
                        this.currentRefreshToken = newTokens.refreshToken();
                        System.out.println("Access AND Refresh Tokens refreshed successfully!");
                    } else {
                        System.out.println("Access Token refreshed successfully!");
                    }

                    // Сбрасываем счетчик попыток при успешном обновлении
                    refreshAttempts = 0;
                } else {
                    // Ошибка парсинга ответа сервера
                    System.out.println("Failed to parse new tokens from refresh response. Server returned invalid format.");
                    log.warn("Refresh response parsing failed. Response: {}", refreshResponse.get());

                    // Увеличиваем счетчик неудачных попыток
                    refreshAttempts++;

                    // Если несколько попыток подряд неудачны, считаем ошибку постоянной
                    if (refreshAttempts >= MAX_REFRESH_ATTEMPTS) {
                        System.out.println("Multiple refresh attempts failed due to server response format issues. Please login again.");
                        clearTokens();
                    }
                }
            } else {
                // Сетевая ошибка или сервер не ответил
                System.out.println("Failed to refresh Access Token. Network or server error.");

                // Увеличиваем счетчик неудачных попыток
                refreshAttempts++;

                // Если несколько попыток подряд неудачны, считаем ошибку постоянной
                if (refreshAttempts >= MAX_REFRESH_ATTEMPTS) {
                    System.out.println("Multiple refresh attempts failed due to network issues. Please check your connection and try again.");
                    clearTokens();
                }
            }
        } catch (Exception e) {
            // Непредвиденная ошибка
            System.out.println("Unexpected error during token refresh: " + e.getMessage());
            log.error("Unexpected error during token refresh", e);

            // Увеличиваем счетчик неудачных попыток
            refreshAttempts++;

            // Если несколько попыток подряд неудачны, считаем ошибку постоянной
            if (refreshAttempts >= MAX_REFRESH_ATTEMPTS) {
                System.out.println("Multiple refresh attempts failed due to unexpected errors. Please login again.");
                clearTokens();
            }
        }
    }

    // Обновляем метод performLogin, чтобы сбрасывать счетчик при успешном входе
    private void performLogin() {
        System.out.print("Enter username: ");
        String username = this.scanner.nextLine();
        System.out.print("Enter password: ");
        String password = this.scanner.nextLine();

        System.out.println("Attempting to log in...");
        Optional<String> loginResponse = httpClientService.performBasicAuthLogin(
                URI.create(serverConfig.getFullLoginUrl()), username, password
        );

        if (loginResponse.isPresent()) {
            Optional<Tokens> tokensOpt = jwtTokenHandler.extractTokensFromResponse(loginResponse.get());
            if (tokensOpt.isPresent()) {
                Tokens tokens = tokensOpt.get();
                this.currentAccessToken = tokens.accessToken();
                this.currentRefreshToken = tokens.refreshToken();
                this.refreshAttempts = 0; // Сбрасываем счетчик при успешном входе

                System.out.println("Login successful!");
                System.out.println("Access Token received.");
                if (this.currentRefreshToken != null) {
                    System.out.println("Refresh Token received.");
                } else {
                    System.out.println("No Refresh Token was provided by the server.");
                }
            } else {
                System.out.println("Login failed: Could not extract tokens from server response.");
                log.warn("Login response did not contain valid tokens: {}", loginResponse.get());
            }
        } else {
            System.out.println("Login failed: No response or HTTP error.");
        }
    }


    private boolean isTokenValid(String token) {
        // TODO добавить проверку срока действия токена
        return token != null && !token.isEmpty();
    }
}