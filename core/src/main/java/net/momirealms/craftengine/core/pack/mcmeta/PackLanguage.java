package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public final class PackLanguage {
    private final Map<String, Entry> languages;

    public PackLanguage(JsonObject language) {
        Map<String, Entry> languages = new HashMap<>(language.size());
        for (Map.Entry<String, JsonElement> entry : language.entrySet()) {
            JsonObject definition = entry.getValue().getAsJsonObject();
            languages.put(entry.getKey(), new Entry(
                    definition.get("name").getAsString(),
                    definition.get("region").getAsString(),
                    definition.has("bidirectional") && definition.get("bidirectional").getAsBoolean()
            ));
        }
        this.languages = Map.copyOf(languages);
    }

    public Map<String, Entry> languages() {
        return this.languages;
    }

    public record Entry(String name, String region, boolean bidirectional) {
    }
}
