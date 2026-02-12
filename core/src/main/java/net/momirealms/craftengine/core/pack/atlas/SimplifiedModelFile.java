package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public class SimplifiedModelFile {
    public final Key parent;
    public final Map<String, Key> textures;

    public SimplifiedModelFile(JsonObject model) {
        this.textures = new HashMap<>();
        if (model.has("textures")) {
            JsonObject textures = model.get("textures").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String value = entry.getValue().getAsString();
                if (value.isEmpty() || value.charAt(0) == '#') continue;
                this.textures.put(entry.getKey(), Key.of(value));
            }
        }
        if (model.has("parent")) {
            this.parent = Key.of(model.get("parent").getAsString());
        } else {
            this.parent = null;
        }
    }
}
