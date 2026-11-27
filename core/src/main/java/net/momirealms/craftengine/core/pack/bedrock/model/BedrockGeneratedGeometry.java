package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.BitSet;

public final class BedrockGeneratedGeometry {
    private static final int UP = 1;
    private static final int DOWN = 2;
    private static final int WEST = 4;
    private static final int EAST = 8;

    public enum EdgeMerging {
        NONE, PER_PIXEL, SOLID_TEXTURE
    }

    private BedrockGeneratedGeometry() {
    }

    public static void addLayer(JsonArray cubes, Outline outline, @Nullable String material, int layer, int textureWidth, int textureHeight, EdgeMerging merging) {
        int width = outline.width;
        int height = outline.height;
        float depth = 0.5f + layer / 1024f;
        float u = 16f * width / textureWidth;
        float v = 16f * height / textureHeight;
        JsonObject faces = new JsonObject();
        faces.add("south", face(material, 0, 0, u, v));
        faces.add("north", face(material, u, 0, -u, v));
        if (merging == EdgeMerging.SOLID_TEXTURE && outline.uniformOpaque) {
            // 单色、不透明的独立静态贴图在任一边缘都采到相同颜色，四个完整侧面与逐像素侧面覆盖相同的顶点平面。
            float x = 1.6f / textureWidth;
            float y = 1.6f / textureHeight;
            float su = 12.8f / textureWidth;
            float sv = 12.8f / textureHeight;
            faces.add("up", face(material, x + su, y + sv, -su, -sv));
            faces.add("down", face(material, x + su, y + sv, -su, -sv));
            faces.add("west", face(material, x, y, su, sv));
            faces.add("east", face(material, x, y, su, sv));
            cube(cubes, -8, 0, -depth, 16, 16, depth * 2, faces);
            return;
        }
        cube(cubes, -8, 0, -depth, 16, 16, depth * 2, faces);
        byte[] edges = outline.edges;
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
                    int sides = merging == EdgeMerging.NONE ? Integer.lowestOneBit(edge) : edge;
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

    public static final class Outline {
        private final int width;
        private final int height;
        private final byte[] edges;
        private final boolean uniformOpaque;

        public Outline(TextureData data) {
            this.width = data.image().getWidth();
            this.height = data.image().getHeight();
            this.edges = new byte[this.width * this.height];
            int[] pixels = new int[this.edges.length];
            TextureAnimation animation = data.animation();
            if (animation == null) {
                collectEdges(data.image(), 0, 0, this.width, this.height, pixels, this.edges);
                int color = pixels[0];
                this.uniformOpaque = data.vanillaPath() == null && (color >>> 24) == 255
                        && Arrays.mismatch(pixels, 0, pixels.length - 1, pixels, 1, pixels.length) == -1;
            } else {
                this.uniformOpaque = false;
                BitSet visited = new BitSet();
                int columns = animation.sheet().getWidth() / this.width;
                // 轮廓是所有播放帧的并集。同一图集格只取一次像素。
                for (TextureAnimation.Frame frame : animation.frames()) {
                    if (!visited.get(frame.index())) {
                        visited.set(frame.index());
                        collectEdges(animation.sheet(), frame.index() % columns * this.width, frame.index() / columns * this.height, this.width, this.height, pixels, this.edges);
                    }
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
