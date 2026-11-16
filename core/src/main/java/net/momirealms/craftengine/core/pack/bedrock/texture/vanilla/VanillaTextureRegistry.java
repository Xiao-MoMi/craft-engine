package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    void flipbook(String tile, int index, String path, int interval, int replicate) {
        this.flipbook(tile, index, path, interval, replicate, List.of(), false);
    }

    void flipbook(String tile, int index, String path, int interval, int replicate, List<Integer> frames, boolean blend) {
        JsonObject entry = new JsonObject();
        entry.addProperty("atlas_tile", tile);
        if (index >= 0) {
            entry.addProperty("atlas_index", index);
            entry.addProperty("atlas_tile_variant", index);
        }
        entry.addProperty("flipbook_texture", path);
        entry.addProperty("ticks_per_frame", interval);
        entry.addProperty("blend_frames", blend);
        if (!frames.isEmpty()) {
            JsonArray sequence = new JsonArray(frames.size());
            for (int frame : frames) {
                sequence.add(frame);
            }
            entry.add("frames", sequence);
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

    void write(Path root) throws IOException {
        if (this.flipbooks.isEmpty()) {
            return;
        }
        Path file = root.resolve(BedrockPackLayout.FLIPBOOK_TEXTURES);
        Map<String, JsonObject> merged = new LinkedHashMap<>();
        if (Files.isRegularFile(file)) {
            for (JsonElement element : GsonHelper.readJsonFromFile(file).getAsJsonArray()) {
                JsonObject entry = element.getAsJsonObject();
                int index = entry.has("atlas_index") ? entry.get("atlas_index").getAsInt() : 0;
                merged.put(entry.get("atlas_tile").getAsString() + ':' + Math.max(0, index), entry);
            }
        }
        for (Map.Entry<String, JsonObject> entry : this.flipbooks.entrySet()) {
            JsonObject previous = merged.putIfAbsent(entry.getKey(), entry.getValue());
            if (previous != null && !previous.equals(entry.getValue())) {
                CraftEngine.instance().logger().warn("Keeping existing Bedrock flipbook " + entry.getKey());
            }
        }
        JsonArray entries = new JsonArray(merged.size());
        merged.values().forEach(entries::add);
        Files.createDirectories(file.getParent());
        GsonHelper.writeJsonFile(entries, file);
    }
}
