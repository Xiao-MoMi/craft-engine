package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.awt.image.BufferedImage;
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
        if (!animation.interpolate()) {
            Int2ObjectOpenHashMap<JsonPrimitive> aliases = new Int2ObjectOpenHashMap<>();
            TextureAnimation.Sampler sampler = animation.sampler();
            BufferedImage image = new BufferedImage(animation.width(), animation.height(), BufferedImage.TYPE_INT_ARGB);
            int tick = 0;
            int interval = animation.sampleInterval();
            for (TextureAnimation.Frame frame : animation.frames()) {
                JsonPrimitive alias = aliases.get(frame.index());
                if (alias == null) {
                    String suffix = "_frame_" + frames.size();
                    sampler.render(tick, color, image);
                    ImageUtils.writePng(image, root.resolve(path + suffix + ".png"));
                    textures.addProperty(name + suffix, path + suffix);
                    alias = new JsonPrimitive("Texture." + name + suffix);
                    aliases.put(frame.index(), alias);
                }
                for (int i = 0; i < frame.time() / interval; i++) {
                    frames.add(alias);
                }
                tick += frame.time();
            }
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
