package net.momirealms.craftengine.bedrock.geyser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class GeyserProperties {
    private static final Properties PROPERTIES = load();

    private GeyserProperties() {
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property in craft-engine-geyser.properties: " + key);
        }
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream stream = GeyserProperties.class.getResourceAsStream("/craft-engine-geyser.properties")) {
            if (stream == null) {
                throw new IOException("Missing embedded craft-engine-geyser.properties");
            }
            properties.load(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load craft-engine-geyser.properties", exception);
        }
        return properties;
    }
}