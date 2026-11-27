package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SimplifiedModelFile {
    public final Key parent;
    public final Map<String, Key> textures;
    public final Map<String, String> references;
    public final @Nullable List<FaceTexture> faces;

    public SimplifiedModelFile(JsonObject model) {
        Map<String, Key> directTextures = new HashMap<>();
        Map<String, String> textureReferences = new HashMap<>();
        if (model.has(ModelJsonKeys.TEXTURES)) {
            JsonObject textures = model.get(ModelJsonKeys.TEXTURES).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                JsonElement value = entry.getValue();
                String sprite = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get(ModelJsonKeys.SPRITE).getAsString();
                if (value.isJsonPrimitive() && sprite.startsWith("#")) {
                    textureReferences.put(entry.getKey(), sprite.substring(1));
                } else {
                    directTextures.put(entry.getKey(), Key.of(sprite));
                }
            }
        }
        this.textures = directTextures.isEmpty() ? Map.of() : Collections.unmodifiableMap(directTextures);
        this.references = textureReferences.isEmpty() ? Map.of() : Collections.unmodifiableMap(textureReferences);
        if (model.has(ModelJsonKeys.PARENT)) {
            this.parent = Key.of(model.get(ModelJsonKeys.PARENT).getAsString());
        } else {
            this.parent = null;
        }
        this.faces = readFaces(model);
    }

    public static @Nullable List<FaceTexture> readFaces(JsonObject model) {
        if (!(model.get(ModelJsonKeys.ELEMENTS) instanceof JsonArray elements)) {
            return null;
        }
        List<FaceTexture> references = new ArrayList<>();
        for (int index = 0; index < elements.size(); index++) {
            JsonObject element = elements.get(index).getAsJsonObject();
            if (!(element.get(ModelJsonKeys.FACES) instanceof JsonObject faces)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                JsonElement texture = entry.getValue().getAsJsonObject().get(ModelJsonKeys.TEXTURE);
                if (texture != null && texture.isJsonPrimitive()) {
                    String slot = texture.getAsString();
                    references.add(new FaceTexture(index, entry.getKey(), slot.startsWith("#") ? slot.substring(1) : slot));
                }
            }
        }
        return List.copyOf(references);
    }

    public record FaceTexture(int element, String face, String slot) {

        public String location() {
            return ModelJsonKeys.ELEMENTS + "[" + this.element + "]." + ModelJsonKeys.FACES + "." + this.face + "." + ModelJsonKeys.TEXTURE;
        }
    }
}
