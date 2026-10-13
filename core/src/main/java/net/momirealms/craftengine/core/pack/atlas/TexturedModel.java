package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public final class TexturedModel {
    public static final TexturedModel EMPTY = new TexturedModel(Key.of("minecraft:missingno"), Map.of());
    public static final TexturedModel BUILTIN = new TexturedModel(Key.of("minecraft:builtin"), Map.of());
    public static final TexturedModel GENERATED = new TexturedModel(Key.of("minecraft:builtin/generated"), Map.of(), Map.of(ModelJsonKeys.PARTICLE, "layer0"), List.of());
    public final Key path;
    public final Map<String, Key> textures;
    private TextureDefinitions definitions;
    private Map<String, Key> resolvedTextures;
    private FaceTextures faces;

    public TexturedModel(Key path, Map<String, Key> textures) {
        this(path, Map.copyOf(textures), Map.of(), null);
    }

    public TexturedModel(Key path, SimplifiedModelFile model) {
        this(path, model.textures, model.references, model.faces);
    }

    private TexturedModel(Key path, Map<String, Key> textures, Map<String, String> references, List<SimplifiedModelFile.FaceTexture> faces) {
        this.path = path;
        this.definitions = new TextureDefinitions(textures, references, null);
        this.faces = faces == null ? null : new FaceTextures(faces);
        this.textures = new TextureView();
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
        // Capture immutable definitions, not resolved values or a mutable parent link.
        // Aliases see child overrides; even cyclic parent files produce a finite chain.
        this.definitions = new TextureDefinitions(this.definitions.textures, this.definitions.references, parent.definitions);
        if (this.faces == null) {
            this.faces = parent.faces;
        }
        this.resolvedTextures = null;
    }

    private Map<String, Key> resolveTextures() {
        if (this.resolvedTextures != null) {
            return this.resolvedTextures;
        }
        if (this.definitions.parent == null && this.definitions.references.isEmpty()) {
            this.resolvedTextures = this.definitions.textures;
            return this.resolvedTextures;
        }
        Map<String, Key> resolved = new HashMap<>(this.definitions.textures);
        for (TextureDefinitions definitions = this.definitions; definitions != null; definitions = definitions.parent) {
            for (Map.Entry<String, Key> texture : definitions.textures.entrySet()) {
                if (findDefinition(texture.getKey()) == definitions) {
                    resolved.putIfAbsent(texture.getKey(), texture.getValue());
                }
            }
        }
        Set<String> unresolved = null;
        Set<String> chain = null;
        for (TextureDefinitions definitions = this.definitions; definitions != null; definitions = definitions.parent) {
            for (String slot : definitions.references.keySet()) {
                if (resolved.containsKey(slot)) {
                    continue;
                }
                if (unresolved == null) {
                    unresolved = new HashSet<>();
                    chain = new LinkedHashSet<>();
                }
                resolveTexture(slot, resolved, unresolved, chain);
            }
        }
        this.resolvedTextures = Collections.unmodifiableMap(resolved);
        return this.resolvedTextures;
    }

    private void resolveTexture(String slot, Map<String, Key> resolved, Set<String> unresolved, Set<String> chain) {
        if (resolved.containsKey(slot) || unresolved.contains(slot)) {
            return;
        }
        String current = slot;
        Key texture;
        // Memoize the whole path, including missing and cyclic paths, in this child context.
        while (true) {
            texture = resolved.get(current);
            if (texture != null || unresolved.contains(current) || !chain.add(current)) {
                break;
            }
            TextureDefinitions definitions = findDefinition(current);
            if (definitions == null) {
                break;
            }
            current = definitions.references.get(current);
        }
        if (texture == null) {
            unresolved.addAll(chain);
        } else {
            for (String visited : chain) {
                resolved.put(visited, texture);
            }
        }
        chain.clear();
    }

    @Nullable
    private TextureDefinitions findDefinition(String slot) {
        for (TextureDefinitions definitions = this.definitions; definitions != null; definitions = definitions.parent) {
            if (definitions.textures.containsKey(slot) || definitions.references.containsKey(slot)) {
                return definitions;
            }
        }
        return null;
    }

    public void setTextureReference(String slot, String target) {
        Map<String, Key> textures = this.definitions.textures;
        if (textures.containsKey(slot)) {
            textures = new HashMap<>(textures);
            textures.remove(slot);
            textures = Collections.unmodifiableMap(textures);
        }
        Map<String, String> references = new HashMap<>(this.definitions.references);
        references.put(slot, target);
        this.definitions = new TextureDefinitions(textures, Collections.unmodifiableMap(references), this.definitions.parent);
        this.resolvedTextures = null;
    }

    public Map<String, String> references() {
        Map<String, String> references = new HashMap<>();
        forEachReference(references::put);
        return Collections.unmodifiableMap(references);
    }

    public @Nullable String reference(String slot) {
        TextureDefinitions definitions = findDefinition(slot);
        return definitions == null ? null : definitions.references.get(slot);
    }

    public void forEachReference(BiConsumer<String, String> consumer) {
        for (TextureDefinitions definitions = this.definitions; definitions != null; definitions = definitions.parent) {
            for (Map.Entry<String, String> reference : definitions.references.entrySet()) {
                if (findDefinition(reference.getKey()) == definitions) {
                    consumer.accept(reference.getKey(), reference.getValue());
                }
            }
        }
    }

    public List<SimplifiedModelFile.FaceTexture> faces() {
        return this.faces == null ? List.of() : this.faces.values;
    }

    public void refreshFaces(JsonObject model) {
        List<SimplifiedModelFile.FaceTexture> updated = SimplifiedModelFile.readFaces(model);
        if (updated == null) {
            return;
        }
        if (this.faces == null) {
            this.faces = new FaceTextures(updated);
        } else {
            // Replace the session's face snapshot without modifying the preset's immutable list.
            this.faces.values = updated;
        }
    }

    private record TextureDefinitions(Map<String, Key> textures, Map<String, String> references,
                                      @Nullable TextureDefinitions parent) {
    }

    private static final class FaceTextures {
        private List<SimplifiedModelFile.FaceTexture> values;

        private FaceTextures(List<SimplifiedModelFile.FaceTexture> values) {
            this.values = values;
        }
    }

    private final class TextureView extends AbstractMap<String, Key> {

        @Override
        public Key get(Object key) {
            return TexturedModel.this.resolveTextures().get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return TexturedModel.this.resolveTextures().containsKey(key);
        }

        @Override
        public int size() {
            return TexturedModel.this.resolveTextures().size();
        }

        @Override
        public Set<Entry<String, Key>> entrySet() {
            return TexturedModel.this.resolveTextures().entrySet();
        }
    }

    @Override
    public String toString() {
        return "TexturedModel{" +
                "textures=" + this.textures +
                '}';
    }
}
