package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class VanillaTextureRegistry {
    private final Map<String, String> outputs = new HashMap<>();
    private final Map<String, JsonObject> flipbooks = new LinkedHashMap<>();

    boolean claim(String path, String owner) {
        String previous = this.outputs.putIfAbsent(path, owner);
        if (previous == null) {
            return true;
        }
        if (!previous.equals(owner)) {
            CraftEngine.instance().logger().warn("Conflicting Bedrock texture " + path + ": " + previous + " and " + owner);
        }
        return false;
    }

    boolean contains(String path) {
        return this.outputs.containsKey(path);
    }

    void flipbook(String tile, int index, String path, int interval, int replicate, JsonArray frames, boolean blend) {
        JsonObject entry = new JsonObject();
        entry.addProperty("atlas_tile", tile);
        if (index >= 0) {
            entry.addProperty("atlas_index", index);
            entry.addProperty("atlas_tile_variant", index);
        }
        entry.addProperty("flipbook_texture", path);
        entry.addProperty("ticks_per_frame", interval);
        entry.addProperty("blend_frames", blend);
        if (frames != null) {
            entry.add("frames", frames);
        }
        if (replicate != 1) {
            entry.addProperty("replicate", replicate);
        }
        String key = tile + ':' + Math.max(0, index);
        JsonObject previous = this.flipbooks.putIfAbsent(key, entry);
        if (previous != null && !previous.equals(entry)) {
            throw new IllegalArgumentException("Conflicting Bedrock flipbook " + key);
        }
    }

    void contributeTo(BedrockTerrainAtlas atlas) {
        this.flipbooks.values().forEach(atlas::addFallbackFlipbook);
    }
}
