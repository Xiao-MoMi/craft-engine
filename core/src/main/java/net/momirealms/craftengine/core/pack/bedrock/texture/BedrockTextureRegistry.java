package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BedrockTextureRegistry {
    private final JsonObject terrain = new JsonObject();
    private final JsonObject items = new JsonObject();
    private final Map<String, JsonObject> flipbooks = new LinkedHashMap<>();
    private final Map<String, String> outputs = new HashMap<>();

    public boolean claim(String path, String owner) {
        String previous = this.outputs.putIfAbsent(path, owner);
        if (previous == null) {
            return true;
        }
        if (!previous.equals(owner)) {
            CraftEngine.instance().logger().warn("Conflicting Bedrock texture " + path + ": " + previous + " and " + owner);
        }
        return false;
    }

    public boolean contains(String path) {
        return this.outputs.containsKey(path);
    }

    public void terrain(String name, String path) {
        this.add(this.terrain, name, path);
    }

    public void item(String name, String path) {
        this.add(this.items, name, path);
    }

    private void add(JsonObject atlas, String name, String path) {
        JsonObject entry = new JsonObject();
        entry.addProperty("textures", path);
        if (atlas.has(name) && !atlas.get(name).equals(entry)) {
            throw new IllegalArgumentException("Conflicting Bedrock atlas entry " + name);
        }
        atlas.add(name, entry);
    }

    public void flipbook(String tile, int index, String path, int interval) {
        this.flipbook(tile, index, path, interval, 1);
    }

    public void flipbook(String tile, int index, String path, int interval, int replicate) {
        JsonObject entry = new JsonObject();
        entry.addProperty("atlas_tile", tile);
        if (index >= 0) {
            entry.addProperty("atlas_index", index);
            entry.addProperty("atlas_tile_variant", index);
        }
        entry.addProperty("flipbook_texture", path);
        entry.addProperty("ticks_per_frame", interval);
        entry.addProperty("blend_frames", false);
        if (replicate != 1) {
            entry.addProperty("replicate", replicate);
        }
        String key = tile + ':' + Math.max(0, index);
        JsonObject previous = this.flipbooks.putIfAbsent(key, entry);
        if (previous != null && !previous.equals(entry)) {
            throw new IllegalArgumentException("Conflicting Bedrock flipbook " + key);
        }
    }

    public void write(Path root) throws IOException {
        Path directory = Files.createDirectories(root.resolve("textures"));
        this.writeAtlas(directory.resolve("item_texture.json"), "atlas.items", this.items);
        if (!this.terrain.isEmpty()) {
            this.writeAtlas(directory.resolve("terrain_texture.json"), "atlas.terrain", this.terrain);
        }
        if (!this.flipbooks.isEmpty()) {
            JsonArray entries = new JsonArray();
            this.flipbooks.values().forEach(entries::add);
            GsonHelper.writeJsonFile(entries, directory.resolve("flipbook_textures.json"));
        }
    }

    private void writeAtlas(Path path, String name, JsonObject entries) throws IOException {
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", name);
        if (name.equals("atlas.terrain")) {
            atlas.addProperty("padding", 8);
            atlas.addProperty("num_mip_levels", 4);
        }
        atlas.add("texture_data", entries);
        GsonHelper.writeJsonFile(atlas, path);
    }
}
