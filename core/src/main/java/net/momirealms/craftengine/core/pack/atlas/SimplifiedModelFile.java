package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public final class SimplifiedModelFile {
    public final Key parent;
    public final Map<String, Key> textures;

    public SimplifiedModelFile(JsonObject model) {
        this.textures = new HashMap<>();
        if (model.has("textures")) {
            JsonObject textures = model.get("textures").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                JsonElement value = entry.getValue();
                String sprite = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("sprite").getAsString();
                if (sprite.isEmpty() || sprite.charAt(0) == '#') continue;
                this.textures.put(entry.getKey(), Key.of(sprite));
            }
        }
        if (model.has("parent")) {
            this.parent = Key.of(model.get("parent").getAsString());
        } else {
            this.parent = null;
        }
    }
}
