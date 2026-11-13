package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class BedrockVanillaTextureReferences {
    private static final Map<String, String> PATHS = load();

    private BedrockVanillaTextureReferences() {
    }

    @Nullable
    public static String resolve(JavaResourcePack pack, Key resource, @Nullable JsonObject metadata) {
        String reference = path(resource);
        if (reference == null || (metadata != null && metadata.has("animation"))) {
            return null;
        }
        String source = ResourcePackUtils.texturePath(resource);
        return pack.hasCustomResource(source) || pack.hasCustomResource(source + ".mcmeta") ? null : reference;
    }

    @Nullable
    private static String path(Key texture) {
        return texture.isMinecraftNamespace() ? PATHS.get(texture.value()) : null;
    }

    private static Map<String, String> load() {
        Map<String, String> paths = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : GsonHelper.readJsonFromResource("/bedrock/vanilla_texture_references.json")
                .getAsJsonObject().getAsJsonObject("textures").entrySet()) {
            paths.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(paths);
    }
}
