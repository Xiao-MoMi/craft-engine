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
    private static final int UP = 1;
    private static final int DOWN = 2;
    private static final int WEST = 4;
    private static final int EAST = 8;

    private BedrockGeneratedGeometry() {
    }

    public static void addLayer(JsonArray cubes, TextureData data, @Nullable String material, int layer, int textureWidth, int textureHeight) {
        addLayer(cubes, data, material, layer, textureWidth, textureHeight, true);
    }

    public static void addLayer(JsonArray cubes, TextureData data, @Nullable String material, int layer, int textureWidth, int textureHeight, boolean mergeEdges) {
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
        float pixelWidth = 16f / width;
        float pixelHeight = 16f / height;
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
                while (edge != 0) {
                    int sides = mergeEdges ? edge : Integer.lowestOneBit(edge);
                    edge ^= sides;
                    JsonObject sideFaces = new JsonObject();
                    if ((sides & UP) != 0) {
                        sideFaces.add("up", face(material, uvX + spanU, uvY + spanV, -spanU, -spanV));
                    }
                    if ((sides & DOWN) != 0) {
                        sideFaces.add("down", face(material, uvX + spanU, uvY + spanV, -spanU, -spanV));
                    }
                    if ((sides & WEST) != 0) {
                        sideFaces.add("west", face(material, uvX, uvY, spanU, spanV));
                    }
                    if ((sides & EAST) != 0) {
                        sideFaces.add("east", face(material, uvX, uvY, spanU, spanV));
                    }
                    boolean horizontalPlane = sides == UP || sides == DOWN;
                    boolean verticalPlane = sides == WEST || sides == EAST;
                    cube(cubes, sides == WEST ? left + pixelWidth : left, sides == UP ? bottom + pixelHeight : bottom, -depth,
                            verticalPlane ? 0 : pixelWidth, horizontalPlane ? 0 : pixelHeight, depth * 2, sideFaces);
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
                    edges[index] |= UP;
                }
                if (y == height - 1 || (pixels[index + width] >>> 24) == 0) {
                    edges[index] |= DOWN;
                }
                if (x == 0 || (pixels[index - 1] >>> 24) == 0) {
                    edges[index] |= WEST;
                }
                if (x == width - 1 || (pixels[index + 1] >>> 24) == 0) {
                    edges[index] |= EAST;
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
