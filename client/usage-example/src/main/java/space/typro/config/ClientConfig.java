package space.typro.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Centralized configuration class for the client application.
 * Loads properties from application.properties.
 */
@Slf4j
public class ClientConfig {
    private static ClientConfig instance;

    public final int httpTimeoutSeconds;
    public final boolean interactive;

    public ClientConfig() {
        val loder = ConfigLoader.getInstance();
        this.httpTimeoutSeconds = loder.getIntProperty("client.http.timeout.seconds", 30);
        this.interactive = loder.getBooleanProperty("client.interactive", true);
        log.info("Client configuration loaded successfully.");
    }

    public static ClientConfig getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                instance = new ClientConfig();
            }
        }
        return instance;
    }
}