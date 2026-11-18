package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.BitSet;

public final class BedrockGeneratedGeometry {

    private BedrockGeneratedGeometry() {
    }

    public static void addLayer(JsonArray cubes, TextureData data, @Nullable String material, int layer, int textureWidth, int textureHeight) {
        int width = data.image().getWidth();
        int height = data.image().getHeight();
        float depth = 0.5f + layer / 1024f;
        float u = 16f * width / textureWidth;
        float v = 16f * height / textureHeight;
        JsonObject faces = new JsonObject();
        faces.add("south", face(material, 0, 0, u, v));
        faces.add("north", face(material, u, 0, -u, v));
        cube(cubes, -8, 0, -depth, 16, 16, depth * 2, faces);
        byte[] edges = new byte[width * height];
        int[] pixels = new int[edges.length];
        TextureAnimation animation = data.animation();
        if (animation == null) {
            collectEdges(data.image(), 0, 0, width, height, pixels, edges);
        } else {
            BitSet visited = new BitSet();
            int columns = animation.sheet().getWidth() / width;
            // 轮廓合并所有播放帧，保留首帧中透明、后续帧中出现的像素边缘。
            for (TextureAnimation.Frame frame : animation.frames()) {
                if (!visited.get(frame.index())) {
                    visited.set(frame.index());
                    collectEdges(animation.sheet(), frame.index() % columns * width, frame.index() / columns * height, width, height, pixels, edges);
                }
            }
        }
        float spanU = 0.8f * 16 / textureWidth;
        float spanV = 0.8f * 16 / textureHeight;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int edge = edges[y * width + x];
                if (edge == 0) {
                    continue;
                }
                float left = 8 - (x + 1) * 16f / width;
                float bottom = 16 - (y + 1) * 16f / height;
                float uvX = (x + 0.1f) * 16 / textureWidth;
                float uvY = (y + 0.1f) * 16 / textureHeight;
                for (int side = 0; side < 4; side++) {
                    if ((edge & (1 << side)) == 0) {
                        continue;
                    }
                    String direction = switch (side) {
                        case 0 -> "up";
                        case 1 -> "down";
                        case 2 -> "west";
                        default -> "east";
                    };
                    JsonObject sideFaces = new JsonObject();
                    sideFaces.add(direction, side < 2
                            ? face(material, uvX + spanU, uvY + spanV, -spanU, -spanV)
                            : face(material, uvX, uvY, spanU, spanV));
                    cube(
                            cubes,
                            left + (side == 2 ? 16f / width : 0),
                            bottom + (side == 0 ? 16f / height : 0),
                            -depth,
                            side < 2 ? 16f / width : 0,
                            side < 2 ? 0 : 16f / height,
                            depth * 2,
                            sideFaces
                    );
                }
            }
        }
    }

    private static void collectEdges(BufferedImage image, int left, int top, int width, int height, int[] pixels, byte[] edges) {
        image.getRGB(left, top, width, height, pixels, 0, width);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((pixels[index] >>> 24) == 0) {
                    continue;
                }
                if (y == 0 || (pixels[index - width] >>> 24) == 0) {
                    edges[index] |= 1;
                }
                if (y == height - 1 || (pixels[index + width] >>> 24) == 0) {
                    edges[index] |= 2;
                }
                if (x == 0 || (pixels[index - 1] >>> 24) == 0) {
                    edges[index] |= 4;
                }
                if (x == width - 1 || (pixels[index + 1] >>> 24) == 0) {
                    edges[index] |= 8;
                }
            }
        }
    }

    private static void cube(JsonArray cubes, float x, float y, float z, float width, float height, float depth, JsonObject faces) {
        JsonObject cube = new JsonObject();
        cube.add("origin", VectorUtils.toJson(x, y, z));
        cube.add("size", VectorUtils.toJson(width, height, depth));
        cube.add("uv", faces);
        cubes.add(cube);
    }

    private static JsonObject face(@Nullable String material, float u, float v, float width, float height) {
        JsonObject face = new JsonObject();
        face.add("uv", VectorUtils.toJson(u, v));
        face.add("uv_size", VectorUtils.toJson(width, height));
        if (material != null) {
            face.addProperty("material_instance", material);
        }
        return face;
    }
}
