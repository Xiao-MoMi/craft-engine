package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.io.IOException;
import java.nio.file.Path;

public final class BedrockItemTextures {
    private BedrockItemTextures() {
    }

    public static JsonArray writeFrames(TextureData data, int color, String name, String path, JsonObject textures, Path root) throws IOException {
        TextureAnimation animation = data.animation();
        JsonArray frames = new JsonArray();
        if (animation == null) {
            textures.addProperty(name, data.staticTexture(color).write(root, path));
            frames.add("Texture." + name);
            return frames;
        }
        animation.forEachFrame(color, (index, image) -> {
            String suffix = "_frame_" + index;
            ImageUtils.writePng(image, root.resolve(path + suffix + ".png"));
            textures.addProperty(name + suffix, path + suffix);
            frames.add("Texture." + name + suffix);
        });
        return frames;
    }

}
