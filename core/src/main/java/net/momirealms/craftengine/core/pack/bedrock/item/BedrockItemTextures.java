package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemTextures {
    private BedrockItemTextures() {
    }

    static JsonArray writeFrames(TextureData data, int color, String name, String path, JsonObject textures, Path root) throws IOException {
        TextureAnimation animation = data.animation();
        JsonArray frames = new JsonArray();
        if (animation == null) {
            BufferedImage image = color == WHITE ? data.image() : ImageUtils.multiplyColor(data.image(), color);
            ImageUtils.writePng(image, root.resolve(path + ".png"));
            textures.addProperty(name, path);
            frames.add("Texture." + name);
        } else {
            int interval = animation.sampleInterval();
            BufferedImage image = new BufferedImage(animation.width(), animation.height(), BufferedImage.TYPE_INT_ARGB);
            for (int tick = 0; tick < animation.duration(); tick += interval) {
                animation.render(tick, color, image);
                String suffix = "_frame_" + tick / interval;
                ImageUtils.writePng(image, root.resolve(path + suffix + ".png"));
                textures.addProperty(name + suffix, path + suffix);
                frames.add("Texture." + name + suffix);
            }
        }
        return frames;
    }
}
