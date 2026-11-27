package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;
import static net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture.MAX_FLIPBOOK_SIZE;

public final class BedrockTerrainAtlas {
    private final JsonObject terrain = new JsonObject();
    private final Map<String, JsonObject> flipbooks = new LinkedHashMap<>();

    public void addFallbackFlipbook(JsonObject entry) {
        // 原版转换结果只补充缺项；物品和世界方块已登记的自定义动画优先，发生冲突时保留原条目。
        String key = flipbookKey(entry);
        JsonObject previous = this.flipbooks.putIfAbsent(key, entry);
        if (previous != null && !previous.equals(entry)) {
            CraftEngine.instance().logger().warn("Keeping existing Bedrock flipbook " + key);
        }
    }

    private static String flipbookKey(JsonObject entry) {
        // 未指定 atlas_index 与显式指定 0 指向同一个槽位，必须使用相同的去重键。
        int index = entry.has("atlas_index") ? entry.get("atlas_index").getAsInt() : 0;
        return entry.get("atlas_tile").getAsString() + ':' + Math.max(0, index);
    }

    public void write(TextureData data, int color, String tile, Path root) throws IOException {
        JsonObject texture = new JsonObject();
        texture.addProperty("textures", this.writeTexture(data, color, tile, root));
        this.terrain.add(tile, texture);
    }

    public static void validateAnimation(TextureData data, String texture) {
        TextureAnimation animation = data.animation();
        if (animation != null) {
            int frameLimit = MAX_FLIPBOOK_SIZE / Math.max(animation.width(), animation.height());
            if (!canBakeInterpolation(animation, frameLimit)) {
                keyFrameInterval(animation, animation.keyFrames(), frameLimit, texture);
            }
        }
    }

    private static boolean canBakeInterpolation(TextureAnimation animation, int frameLimit) {
        return animation.interpolate() && animation.duration() <= frameLimit;
    }

    private static int keyFrameInterval(TextureAnimation animation, TextureAnimation.KeyFrames frames, int frameLimit, String texture) {
        int interval = animation.interpolate() ? animation.uniformFrameTime() : animation.sampleInterval();
        if (interval == 0 || frames.size() > frameLimit) {
            throw new UnsupportedOperationException("texture " + texture + " cannot fit its animation in a " + MAX_FLIPBOOK_SIZE + " px Bedrock flipbook while preserving frame timing");
        }
        return interval;
    }

    private String writeTexture(TextureData data, int color, String tile, Path root) throws IOException {
        String path = "textures/" + tile;
        TextureAnimation animation = data.animation();
        int width = data.image().getWidth();
        int height = data.image().getHeight();
        int size = Math.max(width, height);
        BufferedImage first;
        if (animation == null) {
            BedrockTexture texture = data.staticTexture(color);
            if (width == height) {
                return texture.write(root, path);
            }
            first = texture.image();
        } else {
            int frameLimit = MAX_FLIPBOOK_SIZE / size;
            boolean bakeInterpolation = canBakeInterpolation(animation, frameLimit);
            TextureAnimation.KeyFrames frames = bakeInterpolation ? null : animation.keyFrames();
            int interval = bakeInterpolation ? 1 : keyFrameInterval(animation, frames, frameLimit, tile);
            JsonObject flipbook = new JsonObject();
            BufferedImage strip;
            if (bakeInterpolation) {
                int count = animation.duration() / interval;
                strip = new BufferedImage(size, Math.multiplyExact(size, count), BufferedImage.TYPE_INT_ARGB);
                int[] pixels = new int[width * height];
                animation.forEachFrame(color, (sample, frame) -> {
                    frame.getRGB(0, 0, width, height, pixels, 0, width);
                    strip.setRGB(0, sample * size, width, height, pixels, 0, width);
                });
                first = strip.getSubimage(0, 0, size, size);
            } else {
                strip = frames.strip(color);
                flipbook.add("frames", frames.sequence(interval));
                first = strip.getSubimage(0, 0, size, size);
            }
            ImageUtils.writePng(strip, root.resolve(path + "_anim.png"));
            flipbook.addProperty("atlas_tile", tile);
            flipbook.addProperty("flipbook_texture", path + "_anim");
            flipbook.addProperty("ticks_per_frame", interval);
            flipbook.addProperty("blend_frames", animation.interpolate() && !bakeInterpolation);
            this.flipbooks.put(flipbookKey(flipbook), flipbook);
        }
        if (first.getWidth() != size || first.getHeight() != size) {
            BufferedImage padded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            padded.setRGB(0, 0, width, height, first.getRGB(0, 0, width, height, null, 0, width), 0, width);
            first = padded;
        }
        ImageUtils.writePng(first, root.resolve(path + ".png"));
        return path;
    }

    public void finish(Path root) throws IOException {
        if (!this.terrain.isEmpty()) {
            JsonObject atlas = new JsonObject();
            atlas.addProperty("resource_pack_name", NAMESPACE);
            atlas.addProperty("texture_name", "atlas.terrain");
            atlas.addProperty("padding", 8);
            atlas.addProperty("num_mip_levels", 4);
            atlas.add("texture_data", this.terrain);
            Files.createDirectories(root.resolve(TERRAIN_TEXTURE_ATLAS).getParent());
            GsonHelper.writeJsonFile(atlas, root.resolve(TERRAIN_TEXTURE_ATLAS));
        }
        if (!this.terrain.isEmpty() || !this.flipbooks.isEmpty()) {
            JsonArray animations = new JsonArray(this.flipbooks.size());
            this.flipbooks.values().forEach(animations::add);
            Files.createDirectories(root.resolve(FLIPBOOK_TEXTURES).getParent());
            GsonHelper.writeJsonFile(animations, root.resolve(FLIPBOOK_TEXTURES));
        }
    }
}
