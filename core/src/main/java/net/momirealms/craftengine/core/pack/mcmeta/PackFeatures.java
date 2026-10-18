package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;

public final class PackFeatures {
    private final List<Key> enabled;

    public PackFeatures(JsonObject features) {
        JsonArray entries = features.getAsJsonArray("enabled");
        List<Key> enabled = new ArrayList<>(entries.size());
        for (JsonElement entry : entries) {
            enabled.add(Key.of(entry.getAsString()));
        }
        this.enabled = List.copyOf(enabled);
    }

    public List<Key> enabled() {
        return this.enabled;
    }
}
