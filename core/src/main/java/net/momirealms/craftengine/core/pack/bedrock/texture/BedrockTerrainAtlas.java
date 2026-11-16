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

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FLIPBOOK_TEXTURES;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.TERRAIN_TEXTURE_ATLAS;

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
            int interval = animation.sampleInterval(); // 所有帧时长的最大公约数
            int count = animation.duration() / interval;
            BufferedImage strip = new BufferedImage(size, Math.multiplyExact(size, count), BufferedImage.TYPE_INT_ARGB); // 最终动画长条
            int[] pixels = new int[width * height];
            animation.forEachFrame(color, (sample, frame) -> {
                frame.getRGB(0, 0, width, height, pixels, 0, width);
                strip.setRGB(0, sample * size, width, height, pixels, 0, width);
            });
            ImageUtils.writePng(strip, root.resolve(path + "_anim.png"));
            JsonObject flipbook = new JsonObject();
            flipbook.addProperty("atlas_tile", tile);
            flipbook.addProperty("flipbook_texture", path + "_anim");
            flipbook.addProperty("ticks_per_frame", interval);
            flipbook.addProperty("blend_frames", false);
            this.flipbooks.put(flipbookKey(flipbook), flipbook);
            first = strip.getSubimage(0, 0, size, size);
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
            atlas.addProperty("resource_pack_name", "craftengine");
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
