package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public class TexturedModel {
    public static final TexturedModel EMPTY = new TexturedModel(Key.of("minecraft:missingno"), Map.of());
    public static final TexturedModel BUILTIN = new TexturedModel(Key.of("minecraft:builtin"), Map.of());
    public final Key path;
    public final Map<String, Key> textures;

    public TexturedModel(Key path, Map<String, Key> textures) {
        this.textures = new HashMap<>(textures);
        this.path = path;
    }

    public static Map<String, Key> getTextures(JsonObject json) {
        if (json.has("textures")) {
            JsonObject textures = json.get("textures").getAsJsonObject();
            Map<String, Key> map = new HashMap<>(Math.max(textures.size() * 2, 4));
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String value = entry.getValue().getAsString();
                if (value.isEmpty() || value.charAt(0) == '#') continue;
                map.put(entry.getKey(), Key.of(value));
            }
            return map;
        } else {
            return new HashMap<>(4);
        }
    }

    public static Key getParent(JsonObject json) {
        if (json.has("parent")) {
            return Key.of(json.get("parent").getAsString());
        } else {
            return null;
        }
    }

    // 合并父模型的贴图
    public void addParent(TexturedModel parent) {
        if (parent == null || parent == EMPTY) return;
        for (Map.Entry<String, Key> texture : parent.textures.entrySet()) {
            if (!this.textures.containsKey(texture.getKey())) {
                this.textures.put(texture.getKey(), texture.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return "TexturedModel{" +
                "textures=" + this.textures +
                '}';
    }
}
