package net.momirealms.craftengine.core.pack.bedrock.model;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

final class ItemIconRasterizer {
    private final int size;
    private final float[] depth;
    private int[] pixels;

    ItemIconRasterizer(int size) {
        this.size = size;
        this.depth = new float[size * size];
    }

    BufferedImage begin() {
        BufferedImage image = new BufferedImage(this.size, this.size, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        Arrays.fill(this.depth, Float.NEGATIVE_INFINITY);
        return image;
    }

    void triangle(Vertex a, Vertex b, Vertex c, BufferedImage texture, BedrockTextureAtlas.Region region, float light) {
        float area = edge(a, b, c.x, c.y);
        // GUI projection flips Y: front-facing triangles have clockwise screen winding.
        if (area >= 0) {
            return;
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(a.x, Math.min(b.x, c.x))));
        int maxX = Math.min(this.size - 1, (int) Math.ceil(Math.max(a.x, Math.max(b.x, c.x))));
        int minY = Math.max(0, (int) Math.floor(Math.min(a.y, Math.min(b.y, c.y))));
        int maxY = Math.min(this.size - 1, (int) Math.ceil(Math.max(a.y, Math.max(b.y, c.y))));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float first = edge(b, c, x + 0.5f, y + 0.5f) / area;
                float second = edge(c, a, x + 0.5f, y + 0.5f) / area;
                float third = 1 - first - second;
                if (first < 0 || second < 0 || third < 0) {
                    continue;
                }
                float z = first * a.z + second * b.z + third * c.z;
                int index = y * this.size + x;
                if (z <= this.depth[index]) {
                    continue;
                }
                float u = first * a.u + second * b.u + third * c.u;
                float v = first * a.v + second * b.v + third * c.v;
                int textureX = region.x() + Math.clamp((int) Math.floor(u * region.width() / 16), 0, region.width() - 1);
                int textureY = region.y() + Math.clamp((int) Math.floor(v * region.height() / 16), 0, region.height() - 1);
                int color = texture.getRGB(textureX, textureY);
                // Static cutout rendering: transparent texels must not occlude geometry behind them.
                if ((color >>> 24) < 26) {
                    continue;
                }
                int red = Math.round(((color >>> 16) & 255) * light);
                int green = Math.round(((color >>> 8) & 255) * light);
                int blue = Math.round((color & 255) * light);
                this.pixels[index] = 0xFF000000 | (red << 16) | (green << 8) | blue;
                this.depth[index] = z;
            }
        }
    }

    private static float edge(Vertex a, Vertex b, float x, float y) {
        return (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x);
    }

    static final class Vertex {
        float x;
        float y;
        float z;
        float u;
        float v;
    }
}
