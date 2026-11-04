package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ImageUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

final class BedrockDyedItemIcon {
    private static final int SIZE = 64;
    private static final float RADIANS = (float) Math.PI / 180;
    private final BufferedImage normal = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage dyed = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    private final float[] depths = new float[SIZE * SIZE];

    BedrockDyedItemIcon() {
        Arrays.fill(this.depths, Float.NEGATIVE_INFINITY);
    }

    private static float maxAbs(float x, float y, float z) {
        return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
    }

    private static int sample(BufferedImage image, JavaModel.Face face, float horizontal, float vertical) {
        float u = horizontal;
        float v = vertical;
        switch (face.rotation()) {
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
        JavaModel.Uv uv = face.uv();
        int x = (int) Math.floor((uv.u1() + u * (uv.u2() - uv.u1())) * image.getWidth() / 16);
        int y = (int) Math.floor((uv.v1() + v * (uv.v2() - uv.v1())) * image.getHeight() / 16);
        return image.getRGB(Math.floorMod(x, image.getWidth()), Math.floorMod(y, image.getHeight()));
    }

    private static Vector3f project(JavaModel.Element element, Direction face, Matrix4f pose, int u, int v) {
        Vector3f corner = switch (face) {
            case SOUTH -> new Vector3f(u, 1 - v, 1);
            case NORTH -> new Vector3f(1 - u, 1 - v, 0);
            case EAST -> new Vector3f(1, 1 - v, 1 - u);
            case WEST -> new Vector3f(0, 1 - v, u);
            case UP -> new Vector3f(u, 1, v);
            case DOWN -> new Vector3f(u, 0, 1 - v);
        };
        corner.mul(new Vector3f(element.to()).sub(element.from())).add(element.from());
        pose.transformPosition(corner);
        return corner.set((8 + corner.x) * SIZE / 16, (8 - corner.y) * SIZE / 16, corner.z);
    }

    void render(JavaModel model, ItemRenderTarget.Model target, JavaPackTextureSource textures) throws IOException {
        JavaModel.Transform gui = model.display().get(DisplayPosition.GUI);
        Matrix4f view = new Matrix4f().translation(gui.translation())
                .rotateXYZ(gui.rotation().x * RADIANS, gui.rotation().y * RADIANS, gui.rotation().z * RADIANS)
                .scale(gui.scale()).translate(-8, -8, -8);
        for (JavaModel.Element element : model.elements()) {
            Matrix4f pose = new Matrix4f(view);
            JavaModel.Rotation rotation = element.rotation();
            if (rotation != null) {
                Vector3f angles = rotation.angles();
                Matrix4f local = new Matrix4f().rotationZYX(angles.z * RADIANS, angles.y * RADIANS, angles.x * RADIANS);
                if (rotation.rescale()) {
                    local.scale(1 / maxAbs(local.m00(), local.m01(), local.m02()), 1 / maxAbs(local.m10(), local.m11(), local.m12()), 1 / maxAbs(local.m20(), local.m21(), local.m22()));
                }
                pose.translate(rotation.origin()).mul(local).translate(-rotation.origin().x, -rotation.origin().y, -rotation.origin().z);
            }
            for (Map.Entry<Direction, JavaModel.Face> entry : element.faces().entrySet()) {
                JavaModel.Face face = entry.getValue();
                Tint tint = face.tintIndex() >= 0 && face.tintIndex() < target.tints().size() ? target.tints().get(face.tintIndex()) : null;
                int color = tint == null ? 0xffffff : textures.resolveTint(tint);
                this.drawFace(element, entry.getKey(), pose, face, textures.read(face.texture(), TextureUsage.ITEM).image(), color, tint != null && textures.dyeColor(tint) != null);
            }
        }
    }

    private void drawFace(JavaModel.Element element,
                          Direction face,
                          Matrix4f pose,
                          JavaModel.Face source,
                          BufferedImage texture,
                          int color,
                          boolean dyeable) {
        Vector3f origin = project(element, face, pose, 0, 0);
        Vector3f across = project(element, face, pose, 1, 0).sub(origin);
        Vector3f down = project(element, face, pose, 0, 1).sub(origin);
        float determinant = across.x * down.y - across.y * down.x;
        if (determinant <= 0.00001f) {
            return;
        }
        int x0 = Math.max(0, (int) Math.floor(origin.x + Math.min(0, across.x) + Math.min(0, down.x)));
        int y0 = Math.max(0, (int) Math.floor(origin.y + Math.min(0, across.y) + Math.min(0, down.y)));
        int x1 = Math.min(SIZE, (int) Math.ceil(origin.x + Math.max(0, across.x) + Math.max(0, down.x)));
        int y1 = Math.min(SIZE, (int) Math.ceil(origin.y + Math.max(0, across.y) + Math.max(0, down.y)));
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                float dx = x + 0.5f - origin.x;
                float dy = y + 0.5f - origin.y;
                float u = (dx * down.y - dy * down.x) / determinant;
                float v = (across.x * dy - across.y * dx) / determinant;
                float depth = origin.z + u * across.z + v * down.z;
                int index = y * SIZE + x;
                if (u < 0 || u >= 1 || v < 0 || v >= 1 || depth < this.depths[index]) {
                    continue;
                }
                int pixel = sample(texture, source, u, v);
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
