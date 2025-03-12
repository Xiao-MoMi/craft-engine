package net.momirealms.craftengine.core.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PluginProperties {
    private final HashMap<String, String> propertyMap;

    private PluginProperties(HashMap<String, String> propertyMap) {
        this.propertyMap = propertyMap;
    }

    public static String getValue(String key) {
        if (!SingletonHolder.INSTANCE.propertyMap.containsKey(key)) {
            throw new RuntimeException("Unknown key: " + key);
        }
        return SingletonHolder.INSTANCE.propertyMap.get(key);
    }

    private static class SingletonHolder {

        private static final PluginProperties INSTANCE = getInstance();

        private static PluginProperties getInstance() {
             try (InputStream inputStream = PluginProperties.class.getClassLoader().getResourceAsStream("craft-engine.properties")) {
                 HashMap<String, String> versionMap = new HashMap<>();
                 Properties properties = new Properties();
                 properties.load(inputStream);
                 for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                     if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                         versionMap.put(key, value);
                     }
                 }
                 return new PluginProperties(versionMap);
             } catch (IOException e) {
                 throw new RuntimeException(e);
             }
        }
    }
}
