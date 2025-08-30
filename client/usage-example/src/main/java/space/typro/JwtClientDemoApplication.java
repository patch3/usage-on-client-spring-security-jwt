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

    private void run() {
        while (true) {
            System.out.println("\n--- JWT Client Demo Menu ---");
            System.out.println("1. Login");
            if (currentAccessToken != null) {
                System.out.println("2. Access Protected Resource (using current Access Token)");
                // TODO
            }
            if (currentRefreshToken != null) {
                System.out.println("3. Refresh Access Token (using current Refresh Token)");
                // TODO
            }
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            String choice = this.scanner.nextLine();

            switch (choice) {
                case "1": // 1. Login
                    performLogin();
                    break;
                case "2":
                    if (currentAccessToken != null) {
                        accessProtectedResource();
                    } else {
                        System.out.println("You need to log in first");
                        //TODO
                    }
                    break;
                case "3":
                    if (currentRefreshToken != null) {
                        refreshAccessToken();
                    } else {
                        System.out.println("You need to log in first to get a refresh token");
                        //TODO
                    }
                    break;
                case "4":
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option. Please try again");
            }
        }
    }

    private void performLogin() {
        System.out.print("Enter username: ");
        String username = this.scanner.nextLine();
        System.out.print("Enter password: ");
        String password = this.scanner.nextLine();

        System.out.println("Attempting to log in...");
        Optional<String> loginResponse = httpClientService.performBasicAuthLogin(
                URI.create(serverConfig.getFullLoginUrl()
                ), username, password
        );

        if (loginResponse.isPresent()) {
            Optional<Tokens> tokensOpt = jwtTokenHandler.extractTokensFromResponse(loginResponse.get());
            if (tokensOpt.isPresent()) {
                Tokens tokens = tokensOpt.get();
                this.currentAccessToken = tokens.accessToken();
                this.currentRefreshToken = tokens.refreshToken();

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

    private void accessProtectedResource() {
        System.out.println("Accessing protected resource...");
        Optional<String> resourceResponse = httpClientService.getWithBearerToken(
                URI.create(serverConfig.getFullProtectedResourceUrl()), currentAccessToken);

        resourceResponse.ifPresentOrElse(
                response -> {
                    System.out.println("Successfully accessed protected resource!");
                    System.out.println("Response: " + response);
                },
                () -> {
                    System.out.println("Failed to access protected resource. Access token might be invalid or expired.");
                }
        );
    }

    private void refreshAccessToken() {
        System.out.println("Refreshing Access Token...");
        // Клиент просто отправляет Refresh Token на сервер
        Optional<String> refreshResponse = httpClientService.postWithBearerToken(
                URI.create(serverConfig.getFullRefreshUrl()), "{}", currentRefreshToken);

        if (refreshResponse.isPresent()) {
            // Извлекаем новый Access Token из ответа сервера
            Optional<Tokens> newTokensOpt = jwtTokenHandler.extractTokensFromResponse(refreshResponse.get());
            if (newTokensOpt.isPresent()) {
                this.currentAccessToken = newTokensOpt.get().accessToken();
                // Сервер *может* вернуть новый Refresh Token
                if (newTokensOpt.get().refreshToken() != null) {
                    this.currentRefreshToken = newTokensOpt.get().refreshToken();
                    System.out.println("Access AND Refresh Tokens refreshed successfully!");
                } else {
                    System.out.println("Access Token refreshed successfully!");
                }

                // Test access with new token
                System.out.println("Testing access with the new token...");
                Optional<String> testResponse = httpClientService.getWithBearerToken(
                        URI.create(serverConfig.getFullProtectedResourceUrl()), currentAccessToken);
                if (testResponse.isPresent()) {
                    System.out.println("Test successful with new token!");
                } else {
                    System.out.println("Test failed with new token. Server rejected it.");
                }

            } else {
                System.out.println("Failed to parse new tokens from refresh response.");
                this.currentAccessToken = null;
                this.currentRefreshToken = null;
            }
        } else {
            System.out.println("Failed to refresh Access Token. Refresh request failed.");
            this.currentAccessToken = null;
            this.currentRefreshToken = null;
            System.out.println("Cleared local token storage due to refresh failure.");
        }
    }
}