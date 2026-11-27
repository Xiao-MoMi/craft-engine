package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.ImageUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public record DisplayDyeMasks(TextureData texture, BitSet coverage) {

    static DisplayDyeMasks from(TextureData texture, JsonArray bones) {
        int width = texture.image().getWidth();
        int height = texture.image().getHeight();
        BitSet coverage = new BitSet(width * height);
        for (JsonElement bone : bones) {
            JsonArray cubes = bone.getAsJsonObject().getAsJsonArray("cubes");
            if (cubes == null) {
                continue;
            }
            for (JsonElement cube : cubes) {
                for (JsonElement value : cube.getAsJsonObject().getAsJsonObject("uv").asMap().values()) {
                    cover(coverage, value.getAsJsonObject(), width, height);
                }
            }
        }
        return new DisplayDyeMasks(texture, coverage);
    }

    static boolean cover(BitSet coverage, JsonObject face, int width, int height) {
        JsonArray uv = face.getAsJsonArray("uv");
        JsonArray size = face.getAsJsonArray("uv_size");
        double u = uv.get(0).getAsDouble();
        double v = uv.get(1).getAsDouble();
        double endU = u + size.get(0).getAsDouble();
        double endV = v + size.get(1).getAsDouble();
        int left = firstPixel(u, endU, width);
        int right = endPixel(u, endU, width, left);
        int top = firstPixel(v, endV, height);
        int bottom = endPixel(v, endV, height, top);
        for (int y = top; y < bottom; y++) {
            coverage.set(y * width + left, y * width + right);
        }
        return Math.min(u, endU) >= 0 && Math.max(u, endU) <= 16 && Math.min(v, endV) >= 0 && Math.max(v, endV) <= 16;
    }

    private static int firstPixel(double start, double end, int size) {
        return Math.clamp((int) Math.floor(Math.min(start, end) * size / 16), 0, size - 1);
    }

    private static int endPixel(double start, double end, int size, int first) {
        return Math.clamp((int) Math.ceil(Math.max(start, end) * size / 16), first + 1, size);
    }

    public Int2ObjectMap<JsonArray> write(String prefix, String directory, JsonObject textures, Path root, int maxLayers) throws IOException {
        return this.write(prefix, directory, textures, root, maxLayers, null);
    }

    Int2ObjectMap<JsonArray> write(String prefix, String directory, JsonObject textures, Path root, int maxLayers, @Nullable Int2IntMap prepared) throws IOException {
        int width = this.texture.image().getWidth();
        int height = this.texture.image().getHeight();
        int[] source = new int[width * height];
        Int2IntMap palette = prepared == null ? this.palette(maxLayers, source) : prepared;
        Int2ObjectMap<JsonArray> frames = new Int2ObjectRBTreeMap<>();
        for (int shade : palette.values()) {
            if (!frames.containsKey(shade)) {
                frames.put(shade, new JsonArray());
            }
        }
        if (frames.isEmpty()) {
            return frames;
        }
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) mask.getRaster().getDataBuffer()).getData();
        int[] shades = new int[source.length];
        this.texture.forEachFrame(WHITE, (index, frame) -> {
            frame.getRGB(0, 0, width, height, source, 0, width);
            for (int i = this.coverage.nextSetBit(0); i >= 0; i = this.coverage.nextSetBit(i + 1)) {
                shades[i] = palette.get(source[i] & 0xffffff);
            }
            for (Int2ObjectMap.Entry<JsonArray> entry : frames.int2ObjectEntrySet()) {
                int shade = entry.getIntKey();
                for (int i = this.coverage.nextSetBit(0); i >= 0; i = this.coverage.nextSetBit(i + 1)) {
                    pixels[i] = shades[i] == shade ? source[i] : 0;
                }
                writeFrame(prefix, directory, textures, root, shade, index, mask, entry.getValue());
            }
        });
        return frames;
    }

    Int2IntMap palette(int maxLayers, int[] source) throws IOException {
        int width = this.texture.image().getWidth();
        int height = this.texture.image().getHeight();
        Int2LongOpenHashMap frequencies = new Int2LongOpenHashMap();
        // 只统计可染色面采样的非透明像素。动画共用一个调色板，插值帧也参与分组。
        this.texture.forEachFrame(WHITE, (index, frame) -> {
            frame.getRGB(0, 0, width, height, source, 0, width);
            for (int i = this.coverage.nextSetBit(0); i >= 0; i = this.coverage.nextSetBit(i + 1)) {
                if (source[i] >>> 24 != 0) {
                    frequencies.addTo(source[i] & 0xffffff, 1);
                }
            }
        });
        return DisplayDyePalette.group(frequencies, maxLayers);
    }

    static void writeFrame(String prefix,
                           String directory,
                           JsonObject textures,
                           Path root,
                           int shade,
                           int index,
                           BufferedImage mask,
                           JsonArray frames) throws IOException {
        String name = prefix + "_" + Integer.toHexString(shade) + "_" + index;
        String path = directory + name;
        ImageUtils.writePng(mask, root.resolve(path + ".png"));
        textures.addProperty(name, path);
        frames.add("Texture." + name);
    }
}
