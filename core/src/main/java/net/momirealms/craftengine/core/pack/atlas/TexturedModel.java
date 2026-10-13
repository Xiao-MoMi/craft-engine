package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TexturedModel {
    public static final TexturedModel EMPTY = new TexturedModel(Key.of("minecraft:missingno"), Map.of());
    public static final TexturedModel BUILTIN = new TexturedModel(Key.of("minecraft:builtin"), Map.of());
    public static final TexturedModel GENERATED = new TexturedModel(Key.of("minecraft:builtin/generated"), Map.of(), Map.of(ModelJsonKeys.PARTICLE, "layer0"), List.of());
    public final Key path;
    public final Map<String, Key> textures;
    private final Map<String, Key> directTextures;
    private final Map<String, String> references;
    private List<SimplifiedModelFile.FaceTexture> faces;

    public TexturedModel(Key path, Map<String, Key> textures) {
        this(path, textures, Map.of(), null);
    }

    public TexturedModel(Key path, SimplifiedModelFile model) {
        this(path, model.textures, model.references, model.faces);
    }

    private TexturedModel(Key path, Map<String, Key> textures, Map<String, String> references, List<SimplifiedModelFile.FaceTexture> faces) {
        this.path = path;
        this.directTextures = new HashMap<>(textures);
        this.references = new HashMap<>(references);
        this.faces = faces;
        this.textures = new HashMap<>(textures);
        resolveTextures();
    }

    public static Map<String, Key> getTextures(JsonObject json) {
        if (json.has(ModelJsonKeys.TEXTURES)) {
            JsonObject textures = json.get(ModelJsonKeys.TEXTURES).getAsJsonObject();
            Map<String, Key> map = new HashMap<>(Math.max(textures.size() * 2, 4));
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                JsonElement value = entry.getValue();
                String sprite = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get(ModelJsonKeys.SPRITE).getAsString();
                if (sprite.isEmpty() || sprite.charAt(0) == '#') {
                    continue;
                }
                map.put(entry.getKey(), Key.of(sprite));
            }
            return map;
        } else {
            return new HashMap<>(4);
        }
    }

    public static Key getParent(JsonObject json) {
        if (json.has(ModelJsonKeys.PARENT)) {
            return Key.of(json.get(ModelJsonKeys.PARENT).getAsString());
        } else {
            return null;
        }
    }

    public void addParent(TexturedModel parent) {
        if (parent == null || parent == EMPTY) {
            return;
        }
        // Merge definitions, not resolved values: an inherited alias must see child overrides.
        for (Map.Entry<String, Key> texture : parent.directTextures.entrySet()) {
            if (!this.references.containsKey(texture.getKey())) {
                this.directTextures.putIfAbsent(texture.getKey(), texture.getValue());
            }
        }
        for (Map.Entry<String, String> reference : parent.references.entrySet()) {
            if (!this.directTextures.containsKey(reference.getKey())) {
                this.references.putIfAbsent(reference.getKey(), reference.getValue());
            }
        }
        if (this.faces == null) {
            this.faces = parent.faces;
        }
        resolveTextures();
    }

    private void resolveTextures() {
        this.textures.clear();
        this.textures.putAll(this.directTextures);
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, String> entry : this.references.entrySet()) {
                if (this.textures.containsKey(entry.getKey())) {
                    continue;
                }
                Key texture = this.textures.get(entry.getValue());
                if (texture != null) {
                    this.textures.put(entry.getKey(), texture);
                    changed = true;
                }
            }
        } while (changed);
    }

    public void setTextureReference(String slot, String target) {
        this.directTextures.remove(slot);
        this.references.put(slot, target);
        resolveTextures();
    }

    public Map<String, String> references() {
        return this.references;
    }

    public List<SimplifiedModelFile.FaceTexture> faces() {
        return this.faces == null ? List.of() : this.faces;
    }

    public void refreshFaces(JsonObject model) {
        List<SimplifiedModelFile.FaceTexture> updated = SimplifiedModelFile.readFaces(model);
        if (updated == null) {
            return;
        }
        if (this.faces == null) {
            this.faces = updated;
        } else {
            // Children share inherited faces, so persisted repairs must update their view too.
            this.faces.clear();
            this.faces.addAll(updated);
        }
    }

    @Override
    public String toString() {
        return "TexturedModel{" +
                "textures=" + this.textures +
                '}';
    }
}
