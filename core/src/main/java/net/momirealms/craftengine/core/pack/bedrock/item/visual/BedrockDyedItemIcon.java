package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BedrockDyedItemIcon {
    private static final float RADIANS = (float) Math.PI / 180;
    private final int size;
    private final BufferedImage normal;
    private final BufferedImage dyed;
    private final float[] depths;

    BedrockDyedItemIcon(int size) {
        this.size = size;
        this.normal = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        this.dyed = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        this.depths = new float[size * size];
        Arrays.fill(this.depths, Float.NEGATIVE_INFINITY);
    }

    private static int sample(BufferedImage image, JavaModel.Uv uv, int rotation, float horizontal, float vertical) {
        float u = horizontal;
        float v = vertical;
        switch (rotation) {
            case 90 -> {
                u = vertical;
                v = 1 - horizontal;
            }
            case 180 -> {
                u = 1 - horizontal;
                v = 1 - vertical;
            }
            case 270 -> {
                u = 1 - vertical;
                v = horizontal;
            }
        }
        int x = (int) Math.floor((uv.u1() + u * (uv.u2() - uv.u1())) * image.getWidth() / 16);
        int y = (int) Math.floor((uv.v1() + v * (uv.v2() - uv.v1())) * image.getHeight() / 16);
        return image.getRGB(Math.floorMod(x, image.getWidth()), Math.floorMod(y, image.getHeight()));
    }

    private Vector3f project(Vector3f from, Vector3f size, Direction face, Matrix4f pose, int u, int v) {
        Vector3f corner = switch (face) {
            case SOUTH -> new Vector3f(u, 1 - v, 1);
            case NORTH -> new Vector3f(1 - u, 1 - v, 0);
            case EAST -> new Vector3f(1, 1 - v, 1 - u);
            case WEST -> new Vector3f(0, 1 - v, u);
            case UP -> new Vector3f(u, 1, v);
            case DOWN -> new Vector3f(u, 0, 1 - v);
        };
        corner.mul(size).add(from);
        pose.transformPosition(corner);
        return corner.set((8 + corner.x) * this.size / 16, (8 - corner.y) * this.size / 16, corner.z);
    }

    void render(BedrockItemGeometry geometry, List<BedrockItemGeometry.Material> materials, DisplayPosition position) {
        JavaModel.Transform gui = geometry.model().display().getOrDefault(position, JavaModel.Transform.IDENTITY);
        Matrix4f view = new Matrix4f().translation(gui.translation())
                .rotateXYZ(gui.rotation().x * RADIANS, gui.rotation().y * RADIANS, gui.rotation().z * RADIANS)
                .scale(gui.scale()).translate(-8, -8, -8);
        // 局部矩阵使用 0..1 坐标；未烘焙进 cubes 的部分必须在元素旋转之前应用。
        view.scale(16).mul(geometry.transformation()).scale(1f / 16);
        for (JsonElement value : geometry.cubes()) {
            JsonObject cube = value.getAsJsonObject();
            Vector3f origin = VectorUtils.vector3f(cube.getAsJsonArray("origin"));
            Vector3f size = VectorUtils.vector3f(cube.getAsJsonArray("size"));
            Vector3f from = new Vector3f(8 - origin.x - size.x, origin.y, origin.z + 8);
            Matrix4f pose = new Matrix4f(view);
            if (cube.has("rotation")) {
                Vector3f pivot = VectorUtils.vector3f(cube.getAsJsonArray("pivot"));
                pivot.set(8 - pivot.x, pivot.y, pivot.z + 8);
                Vector3f angles = VectorUtils.vector3f(cube.getAsJsonArray("rotation")).mul(RADIANS);
                pose.translate(pivot).rotateZYX(angles.z, -angles.y, -angles.x).translate(-pivot.x, -pivot.y, -pivot.z);
            }
            for (Map.Entry<String, JsonElement> entry : cube.getAsJsonObject("uv").entrySet()) {
                Direction direction = Direction.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                JsonObject face = entry.getValue().getAsJsonObject();
                BedrockItemGeometry.Material material = materials.get(Integer.parseInt(face.get("material_instance").getAsString().substring(5)));
                JsonArray uv = face.getAsJsonArray("uv");
                JsonArray span = face.getAsJsonArray("uv_size");
                float u = uv.get(0).getAsFloat();
                float v = uv.get(1).getAsFloat();
                float endU = u + span.get(0).getAsFloat();
                float endV = v + span.get(1).getAsFloat();
                JavaModel.Uv coordinates = direction.axis() == Direction.Axis.Y
                        ? new JavaModel.Uv(endU, endV, u, v) : new JavaModel.Uv(u, v, endU, endV);
                int rotation = face.has("uv_rotation") ? face.get("uv_rotation").getAsInt() : 0;
                boolean dyed = material.dyeColor() != null;
                this.drawFace(from, size, direction, pose, coordinates, rotation, material.data().image(), dyed ? material.dyeColor() : material.color(), dyed);
            }
        }
    }

    private void drawFace(Vector3f from,
                          Vector3f size,
                          Direction face,
                          Matrix4f pose,
                          JavaModel.Uv uv,
                          int rotation,
                          BufferedImage texture,
                          int color,
                          boolean dyeable) {
        Vector3f origin = this.project(from, size, face, pose, 0, 0);
        Vector3f across = this.project(from, size, face, pose, 1, 0).sub(origin);
        Vector3f down = this.project(from, size, face, pose, 0, 1).sub(origin);
        float determinant = across.x * down.y - across.y * down.x;
        if (determinant <= 0.00001f) {
            return;
        }
        int x0 = Math.max(0, (int) Math.floor(origin.x + Math.min(0, across.x) + Math.min(0, down.x)));
        int y0 = Math.max(0, (int) Math.floor(origin.y + Math.min(0, across.y) + Math.min(0, down.y)));
        int x1 = Math.min(this.size, (int) Math.ceil(origin.x + Math.max(0, across.x) + Math.max(0, down.x)));
        int y1 = Math.min(this.size, (int) Math.ceil(origin.y + Math.max(0, across.y) + Math.max(0, down.y)));
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                float dx = x + 0.5f - origin.x;
                float dy = y + 0.5f - origin.y;
                float u = (dx * down.y - dy * down.x) / determinant;
                float v = (across.x * dy - across.y * dx) / determinant;
                float depth = origin.z + u * across.z + v * down.z;
                int index = y * this.size + x;
                if (u < 0 || u >= 1 || v < 0 || v >= 1 || depth < this.depths[index]) {
                    continue;
                }
                int pixel = sample(texture, uv, rotation, u, v);
                if ((pixel >>> 24) == 0) {
                    continue;
                }
                this.depths[index] = depth;
                int rgb = (pixel >>> 16 & 255) * (color >>> 16 & 255) / 255 << 16
                        | (pixel >>> 8 & 255) * (color >>> 8 & 255) / 255 << 8 | (pixel & 255) * (color & 255) / 255;
                this.normal.setRGB(x, y, 0xff000000 | rgb);
                // 与基岩原版 leather_helmet.tga 一致：染色底层 alpha 为 255，固定装饰为 3，背景为 0。
                this.dyed.setRGB(x, y, dyeable ? (0xff000000 | (pixel & 0xffffff)) : (0x03000000 | rgb));
            }
        }
    }

    void write(Path root, String texture) throws IOException {
        ImageUtils.writePng(this.normal, root.resolve(texture + ".png"));
        ImageUtils.writeTga(this.dyed, root.resolve(texture + "_dyed.tga"));
    }
}
