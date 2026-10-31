package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class BedrockTerrainAtlas {
    private JsonObject terrain = new JsonObject();
    private JsonArray flipbooks = new JsonArray();

    public void read(Path root) throws IOException {
        Path atlas = root.resolve(TERRAIN_TEXTURE_ATLAS);
        if (Files.isRegularFile(atlas)) {
            this.terrain = GsonHelper.readJsonObjectFromFile(atlas).getAsJsonObject("texture_data");
        }
        Path animations = root.resolve(FLIPBOOK_TEXTURES);
        if (Files.isRegularFile(animations)) {
            this.flipbooks = GsonHelper.readJsonFromFile(animations).getAsJsonArray();
        }
    }

    public void write(TextureData data, int color, String tile, Path root) throws IOException {
        String path = "textures/" + tile;
        TextureAnimation animation = data.animation();
        int width = data.image().getWidth();
        int height = data.image().getHeight();
        int size = Math.max(width, height);
        BufferedImage first;
        if (animation == null) {
            first = color == 0xffffff ? data.image() : ImageUtils.multiplyColor(data.image(), color);
        } else {
            int interval = animation.sampleInterval(); // 所有帧时长的最大公约数
            int count = animation.duration() / interval;
            BufferedImage strip = new BufferedImage(size, Math.multiplyExact(size, count), BufferedImage.TYPE_INT_ARGB); // 最终动画长条
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); // 单帧临时画布
            int[] pixels = new int[width * height];
            // 每层独立播放；插值预烘焙到 tick，避免不同时长的帧在 Bedrock 中变速。
            for (int sample = 0; sample < count; sample++) {
                animation.render(sample * interval, color, frame);
                frame.getRGB(0, 0, width, height, pixels, 0, width);
                strip.setRGB(0, sample * size, width, height, pixels, 0, width);
            }
            ImageUtils.writePng(strip, root.resolve(path + "_anim.png"));
            JsonObject flipbook = new JsonObject();
            flipbook.addProperty("atlas_tile", tile);
            flipbook.addProperty("flipbook_texture", path + "_anim");
            flipbook.addProperty("ticks_per_frame", interval);
            flipbook.addProperty("blend_frames", false);
            this.flipbooks.add(flipbook);
            first = strip.getSubimage(0, 0, size, size);
        }
        if (first.getWidth() != size || first.getHeight() != size) {
            BufferedImage padded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            padded.setRGB(0, 0, width, height, first.getRGB(0, 0, width, height, null, 0, width), 0, width);
            first = padded;
        }
        ImageUtils.writePng(first, root.resolve(path + ".png"));
        JsonObject texture = new JsonObject();
        texture.addProperty("textures", path);
        this.terrain.add(tile, texture);
    }

    public void finish(Path root) throws IOException {
        if (this.terrain.isEmpty()) {
            return;
        }
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.terrain");
        atlas.addProperty("padding", 8);
        atlas.addProperty("num_mip_levels", 4);
        atlas.add("texture_data", this.terrain);
        GsonHelper.writeJsonFile(atlas, root.resolve(TERRAIN_TEXTURE_ATLAS));
        GsonHelper.writeJsonFile(this.flipbooks, root.resolve(FLIPBOOK_TEXTURES));
    }
}
