package space.typro.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class ServerConfig {
    private static ServerConfig instance;
    public final String baseUrl;
    public final String loginEndpoint;
    public final String protectedResourceEndpoint;
    public final String refreshEndpoint;

    public ServerConfig() {
        val loder = ConfigLoader.getInstance();
        this.baseUrl = loder.getProperty("server.base.url",
                "https://localhost:8443");
        this.loginEndpoint = loder.getProperty("server.endpoint.login",
                "/jwt/tokens");
        this.protectedResourceEndpoint = loder.getProperty("server.endpoint.protected",
                "/api/protected");
        this.refreshEndpoint = loder.getProperty("server.endpoint.refresh",
                "/api/auth/refresh");
        log.debug("Server configuration loaded successfully!");
    }

    public static ServerConfig getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                instance = new ServerConfig();
            }
        }
        return instance;
    }

    // Convenience methods to build full URIs
    public String getFullLoginUrl() {
        return baseUrl + loginEndpoint;
    }

    public String getFullProtectedResourceUrl() {
        return baseUrl + protectedResourceEndpoint;
    }

    public String getFullRefreshUrl() {
        return baseUrl + refreshEndpoint;
    }
}
