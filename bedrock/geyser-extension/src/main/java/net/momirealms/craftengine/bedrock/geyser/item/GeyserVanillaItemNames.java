package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class GeyserVanillaItemNames {
    private static final String RESOURCE = "/internal/inventory_name_keys.json";
    private static final Map<String, String> NAME_KEYS = load();

    private GeyserVanillaItemNames() {
    }

    @Nullable
    static String nameKey(String javaItemId) {
        return NAME_KEYS.get(javaItemId);
    }

    private static Map<String, String> load() {
        InputStream stream = GeyserVanillaItemNames.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing resource " + RESOURCE);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject items = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, String> names = HashMap.newHashMap(items.size());
            for (Map.Entry<String, JsonElement> item : items.entrySet()) {
                names.put(item.getKey(), item.getValue().getAsString());
            }
            return Map.copyOf(names);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + RESOURCE, exception);
        }
    }
}
