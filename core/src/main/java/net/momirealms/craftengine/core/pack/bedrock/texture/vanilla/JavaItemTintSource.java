package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.model.definition.tint.ConstantTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.GrassTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tints;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

final class JavaItemTintSource {
    private final JavaResourcePack pack;
    private final JavaPackTextureSource textures;

    JavaItemTintSource(JavaResourcePack pack) {
        this.pack = pack;
        this.textures = new JavaPackTextureSource(pack);
    }

    @Nullable
    Integer read(Key key, int tintIndex) throws IOException {
        String path = ResourcePackUtils.itemDefinitionPath(key);
        JsonObject definition = this.pack.readJson(path);
        if (definition == null) {
            return null;
        }
        JsonObject model = definition.getAsJsonObject("model");
        if (model == null || !Key.minecraft("model").equals(Key.of(model.get("type").getAsString()))) {
            throw new IOException("Expected a static item model for " + key);
        }
        JsonArray tints = model.getAsJsonArray("tints");
        if (tintIndex < 0 || tints == null || tintIndex >= tints.size()) {
            return JavaPackTextureSource.WHITE;
        }
        Tint tint = Tints.fromJson(tints.get(tintIndex).getAsJsonObject());
        if (!(tint instanceof ConstantTint) && !(tint instanceof GrassTint)) {
            throw new IOException("Unsupported item tint for " + key + " at index " + tintIndex);
        }
        return this.textures.resolveTint(tint);
    }
}
