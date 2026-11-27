package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

final class DisplayDyeAtlas {
    private static final int MAX_SIDE = 2048;
    private final List<Tile> tiles;
    private final Layout layout;

    private DisplayDyeAtlas(List<Tile> tiles, Layout layout) {
        this.tiles = tiles;
        this.layout = layout;
    }

    record Layer(DisplayDyeMasks masks, Int2IntMap palette, @Nullable DisplayDyeAtlas atlas) {
    }

    private record Palette(int defaultColor, IntList shades) {
    }

    static Map<Integer, Layer> merge(List<BedrockItemGeometry.Material> materials, List<BedrockItemGeometry> geometries,
                                     List<BedrockItemDisplayGenerator.Part> parts, int maxLayers) throws IOException {
        int count = 0;
        for (BedrockItemGeometry.Material material : materials) {
            if (eligible(material)) {
                count++;
            }
        }
        if (count < 2) {
            return Map.of();
        }
        Tile[] layers = new Tile[materials.size()];
        Map<String, Tile> byName = new HashMap<>();
        for (int i = 0; i < layers.length; i++) {
            layers[i] = new Tile(i, materials.get(i));
            byName.put("layer" + i, layers[i]);
        }
        for (int i = 0; i < geometries.size(); i++) {
            BedrockItemGeometry geometry = geometries.get(i);
            Matrix4fc transform = boundsTransform(parts.get(i), geometry.transformation());
            for (JsonElement value : geometry.cubes()) {
                JsonObject cube = value.getAsJsonObject();
                JsonArray size = cube.getAsJsonArray("size");
                boolean solid = size.get(0).getAsFloat() != 0 && size.get(1).getAsFloat() != 0
                        && size.get(2).getAsFloat() != 0 && (transform == null || transform.determinant3x3() > 0);
                Bounds bounds = new Bounds(solid);
                BedrockElementGeometry.bounds(cube, bounds.minimum, bounds.maximum, transform);
                for (JsonElement element : cube.getAsJsonObject("uv").asMap().values()) {
                    JsonObject face = element.getAsJsonObject();
                    byName.get(face.get("material_instance").getAsString()).add(bounds, face);
                }
            }
        }
        // 共享控制器页的实际顺序可能不同于材质编号。重叠面保留在原页，即使它们不在待合并的材质之间。
        for (Tile tile : layers) {
            if (!tile.mergeable) {
                continue;
            }
            for (Tile other : layers) {
                if (tile != other && tile.overlaps(other)) {
                    tile.mergeable = false;
                    other.mergeable = false;
                    break;
                }
            }
        }
        Map<Palette, List<Tile>> groups = new LinkedHashMap<>();
        int[] pixels = new int[0];
        for (Tile tile : layers) {
            if (!tile.mergeable || tile.faces.isEmpty()) {
                continue;
            }
            int capacity = tile.width * tile.height;
            if (pixels.length < capacity) {
                pixels = new int[capacity];
            }
            Palette palette = tile.preparePalette(maxLayers, pixels);
            if (!palette.shades.isEmpty()) {
                groups.computeIfAbsent(palette, ignored -> new ArrayList<>()).add(tile);
            }
        }
        for (List<Tile> group : groups.values()) {
            pack(group);
        }
        Map<Integer, Layer> result = new HashMap<>();
        for (Tile tile : layers) {
            if (tile.palette != null) {
                result.put(tile.index, new Layer(tile.source, tile.palette, tile.atlas));
            }
        }
        return result;
    }

    private static boolean eligible(BedrockItemGeometry.Material material) {
        TextureData data = material.data();
        return material.dyeColor() != null && data.animation() == null && data.vanillaPath() == null && !data.isMissing()
                && data.image().getWidth() <= MAX_SIDE - 2 && data.image().getHeight() <= MAX_SIDE - 2;
    }

    @Nullable
    private static Matrix4fc boundsTransform(BedrockItemDisplayGenerator.Part part, Matrix4fc local) {
        JavaModel.Transform pose = part.display();
        if (pose.equals(JavaModel.Transform.IDENTITY) && BedrockModelTransform.isIdentity(local)) {
            return null;
        }
        float side = part.mirrored() ? -1 : 1;
        float radians = (float) (Math.PI / 180);
        Vector3f position = pose.translation();
        Vector3f rotation = pose.rotation();
        return new Matrix4f().translation(8 + side * position.x, 8 + position.y, 8 + position.z)
                .rotateXYZ(radians * rotation.x, radians * side * rotation.y, radians * side * rotation.z)
                .scale(pose.scale()).translate(-8, -8, -8).scale(16).mul(local).scale(1f / 16);
    }

    private static void pack(List<Tile> tiles) {
        if (tiles.size() < 2) {
            return;
        }
        long area = 0;
        int widest = 0;
        for (Tile tile : tiles) {
            area += (long) (tile.width + 2) * (tile.height + 2);
            widest = Math.max(widest, tile.width + 2);
        }
        int middle = Math.clamp((int) Math.ceil(Math.sqrt(area)), widest, MAX_SIDE);
        Layout best = null;
        for (int width : new int[] {widest, middle, Math.min(MAX_SIDE, middle * 2), MAX_SIDE}) {
            Layout layout = Layout.rows(tiles, width, false);
            if (layout.height <= MAX_SIDE && (best == null || layout.area() < best.area()
                    || (layout.area() == best.area() && layout.longSide() < best.longSide()))) {
                best = layout;
            }
        }
        // 仅为边缘留出一圈采样像素；不同尺寸造成的大块空白会随每张染色蒙版重复分配。
        if (best == null || best.area() > area + area / 4) {
            int half = tiles.size() / 2;
            pack(tiles.subList(0, half));
            pack(tiles.subList(half, tiles.size()));
            return;
        }
        Layout.rows(tiles, best.width, true);
        DisplayDyeAtlas atlas = new DisplayDyeAtlas(tiles, best);
        String material = "layer" + atlas.firstLayer();
        for (Tile tile : tiles) {
            tile.atlas = atlas;
            for (JsonObject face : tile.faces) {
                JsonArray uv = face.getAsJsonArray("uv");
                JsonArray size = face.getAsJsonArray("uv_size");
                face.add("uv", VectorUtils.toJson((16 * tile.x + uv.get(0).getAsFloat() * tile.width) / best.width, (16 * tile.y + uv.get(1).getAsFloat() * tile.height) / best.height));
                face.add("uv_size", VectorUtils.toJson(size.get(0).getAsFloat() * tile.width / best.width, size.get(1).getAsFloat() * tile.height / best.height));
                face.addProperty("material_instance", material);
            }
        }
    }

    int firstLayer() {
        return this.tiles.getFirst().index;
    }

    int[] shades() {
        return this.tiles.getFirst().shades;
    }

    List<DisplayDyeMasks> sources() {
        List<DisplayDyeMasks> sources = new ArrayList<>(this.tiles.size());
        for (Tile tile : this.tiles) {
            sources.add(tile.source);
        }
        return sources;
    }

    record Image(BufferedImage base, int[] pixelShades, int[] shades) {

        Int2ObjectMap<JsonArray> writeMasks(String prefix, String directory, JsonObject textures, Path root) throws IOException {
            int[] pixels = ((DataBufferInt) this.base.getRaster().getDataBuffer()).getData();
            BufferedImage mask = new BufferedImage(this.base.getWidth(), this.base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int[] output = ((DataBufferInt) mask.getRaster().getDataBuffer()).getData();
            Int2ObjectMap<JsonArray> masks = new Int2ObjectRBTreeMap<>();
            for (int shade : this.shades) {
                for (int i = 0; i < pixels.length; i++) {
                    output[i] = this.pixelShades[i] == shade ? pixels[i] : 0;
                }
                JsonArray frames = new JsonArray(1);
                DisplayDyeMasks.writeFrame(prefix, directory, textures, root, shade, 0, mask, frames);
                masks.put(shade, frames);
            }
            return masks;
        }
    }

    Image image() {
        BufferedImage base = new BufferedImage(this.layout.width, this.layout.height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) base.getRaster().getDataBuffer()).getData();
        int[] pixelShades = new int[pixels.length];
        Arrays.fill(pixelShades, -1);
        for (Tile tile : this.tiles) {
            int stride = base.getWidth();
            int start = tile.y * stride + tile.x;
            tile.source.texture().image().getRGB(0, 0, tile.width, tile.height, pixels, start, stride);
            BitSet coverage = tile.source.coverage();
            for (int i = coverage.nextSetBit(0); i >= 0; i = coverage.nextSetBit(i + 1)) {
                int at = start + i / tile.width * stride + i % tile.width;
                pixelShades[at] = tile.palette.get(pixels[at] & 0xffffff);
            }
            // 原图和染色蒙版都复制 RGBA 边缘，透明像素和翻转 UV 不会采到相邻材质。
            tile.extrude(pixels, stride, start);
            tile.extrude(pixelShades, stride, start);
        }
        return new Image(base, pixelShades, this.shades());
    }

    private record Layout(int width, int height) {
        private static Layout rows(List<Tile> tiles, int limit, boolean place) {
            int x = 0;
            int y = 0;
            int rowHeight = 0;
            int width = 0;
            for (Tile tile : tiles) {
                if (x + tile.width + 2 > limit) {
                    y += rowHeight;
                    x = 0;
                    rowHeight = 0;
                }
                if (place) {
                    tile.x = x + 1;
                    tile.y = y + 1;
                }
                x += tile.width + 2;
                width = Math.max(x, width);
                rowHeight = Math.max(rowHeight, tile.height + 2);
            }
            return new Layout(width, y + rowHeight);
        }

        private long area() {
            return (long) this.width * this.height;
        }

        private int longSide() {
            return Math.max(this.width, this.height);
        }
    }

    private record Bounds(Vector3f minimum, Vector3f maximum, boolean solid) {
        private Bounds(boolean solid) {
            this(new Vector3f(Float.POSITIVE_INFINITY), new Vector3f(Float.NEGATIVE_INFINITY), solid);
        }

        private boolean intersects(Bounds other) {
            if (this.solid && other.solid) {
                // 同一实体方块的不同面、相邻实体方块相向的接触面不会同时通过单面剔除并覆盖同一片元。
                return this != other && this.minimum.x < other.maximum.x && this.maximum.x > other.minimum.x
                        && this.minimum.y < other.maximum.y && this.maximum.y > other.minimum.y
                        && this.minimum.z < other.maximum.z && this.maximum.z > other.minimum.z;
            }
            return this.minimum.x <= other.maximum.x && this.maximum.x >= other.minimum.x
                    && this.minimum.y <= other.maximum.y && this.maximum.y >= other.minimum.y
                    && this.minimum.z <= other.maximum.z && this.maximum.z >= other.minimum.z;
        }
    }

    private static final class Tile {
        private final int index;
        private final Integer defaultColor;
        private final int width;
        private final int height;
        private final DisplayDyeMasks source;
        private final List<JsonObject> faces = new ArrayList<>();
        private final Bounds bounds = new Bounds(false);
        private final List<Bounds> cubes = new ArrayList<>();
        private boolean mergeable;
        private Int2IntMap palette;
        private int[] shades;
        private DisplayDyeAtlas atlas;
        private int x;
        private int y;

        private Tile(int index, BedrockItemGeometry.Material material) {
            this.index = index;
            this.defaultColor = material.dyeColor();
            this.mergeable = eligible(material);
            TextureData texture = material.data();
            this.width = texture.image().getWidth();
            this.height = texture.image().getHeight();
            this.source = new DisplayDyeMasks(texture, new BitSet());
        }

        private void add(Bounds cube, JsonObject face) {
            if (this.cubes.isEmpty() || this.cubes.getLast() != cube) {
                this.cubes.add(cube);
                this.bounds.minimum.min(cube.minimum);
                this.bounds.maximum.max(cube.maximum);
            }
            if (this.mergeable) {
                this.mergeable = DisplayDyeMasks.cover(this.source.coverage(), face, this.width, this.height);
                this.faces.add(face);
            }
        }

        private boolean overlaps(Tile other) {
            if (this.bounds.intersects(other.bounds)) {
                for (Bounds cube : this.cubes) {
                    for (Bounds otherCube : other.cubes) {
                        if (cube.intersects(otherCube)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private Palette preparePalette(int maxLayers, int[] pixels) throws IOException {
            this.palette = this.source.palette(maxLayers, pixels);
            IntOpenHashSet representatives = new IntOpenHashSet(Math.min(this.palette.size(), maxLayers));
            representatives.addAll(this.palette.values());
            this.shades = representatives.toIntArray();
            Arrays.sort(this.shades);
            return new Palette(this.defaultColor, IntList.of(this.shades));
        }

        private void extrude(int[] pixels, int stride, int start) {
            for (int y = 0; y < this.height; y++) {
                int row = start + y * stride;
                pixels[row - 1] = pixels[row];
                pixels[row + this.width] = pixels[row + this.width - 1];
            }
            System.arraycopy(pixels, start - 1, pixels, start - stride - 1, this.width + 2);
            System.arraycopy(pixels, start + (this.height - 1) * stride - 1, pixels, start + this.height * stride - 1, this.width + 2);
        }
    }
}
