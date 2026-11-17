package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class VanillaTextureMappings {
    static final VanillaTextureMappings INSTANCE = new VanillaTextureMappings();

    private final Map<String, List<BedrockTextureRule.Tile>> tiles = new HashMap<>();
    private final Map<String, BedrockTextureRule.Tga> tga = new HashMap<>();
    private final Map<String, String> firstFrames = new HashMap<>();
    private final List<BedrockTextureRule> mappings;

    private VanillaTextureMappings() {
        JsonObject json = GsonHelper.readJsonFromResource("/bedrock/vanilla_textures.json").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tiles").entrySet()) {
            List<BedrockTextureRule.Tile> entries = new ArrayList<>(entry.getValue().getAsJsonArray().size());
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                JsonObject tile = element.getAsJsonObject();
                entries.add(new BedrockTextureRule.Tile(tile.get("name").getAsString(), tile.get("index").getAsInt(), tile.has("replicate") ? tile.get("replicate").getAsInt() : 1));
            }
            this.tiles.put(entry.getKey(), List.copyOf(entries));
        }
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tga").entrySet()) {
            JsonObject target = entry.getValue().getAsJsonObject();
            this.tga.put(entry.getKey(), new BedrockTextureRule.Tga(target.get("path").getAsString(), target.get("keep").getAsBoolean()));
        }
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("first_frames").entrySet()) {
            this.firstFrames.put(entry.getKey(), entry.getValue().getAsString());
        }
        List<BedrockTextureRule> mappings = new ArrayList<>(json.getAsJsonObject("textures").size());
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
            List<BedrockTextureRule.Target> targets = new ArrayList<>(entry.getValue().getAsJsonArray().size());
            for (JsonElement destination : entry.getValue().getAsJsonArray()) {
                targets.add(this.target(destination.getAsString()));
            }
            mappings.add(new BedrockTextureRule(entry.getKey() + ".png", List.copyOf(targets)));
        }
        this.mappings = List.copyOf(mappings);
    }

    BedrockTextureRule.Target target(String path) {
        String firstFrame = this.firstFrames.get(path);
        String atlasPath = firstFrame != null ? firstFrame : path;
        String name = atlasPath.substring(0, atlasPath.lastIndexOf('.'));
        return new BedrockTextureRule.Target(path, this.tiles.getOrDefault(name, List.of()), firstFrame);
    }

    @Nullable
    BedrockTextureRule.Tga tga(String path) {
        return this.tga.get(path);
    }

    List<BedrockTextureRule> mappings() {
        return this.mappings;
    }
}
