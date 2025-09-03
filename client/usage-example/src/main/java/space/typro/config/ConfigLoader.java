package space.typro.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.Properties;


@Slf4j
public class ConfigLoader {
    private static volatile ConfigLoader instance;

    private final Properties properties;

    public ConfigLoader() {
        this.properties = new Properties();
        try (val input = getClass().getClassLoader().getResourceAsStream("application.properties")
        ) {
            if (input == null) {
                log.error("Unable to find application.properties");
                throw new RuntimeException("Cannot load application.properties");
            }
            this.properties.load(input);
        } catch (IOException ex) {
            log.error("Error loading application.properties", ex);
            throw new RuntimeException(ex);
        }
        log.debug("Client configuration loaded successfully!");
    }

    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public int getIntProperty(String key, int defaultValue) {
        return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }
}
