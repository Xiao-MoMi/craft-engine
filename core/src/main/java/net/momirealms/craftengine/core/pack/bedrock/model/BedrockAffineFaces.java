package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

public final class BedrockAffineFaces {
    private final Map<Bake, BakedTexture> textures = new HashMap<>();

    public JsonArray transform(JsonArray cubes, Matrix4fc transformation, Map<String, BedrockItemGeometry.Material> materials) throws IOException {
        if (BedrockModelTransform.canBakeCubes(transformation)) {
            BedrockModelTransform.apply(cubes, transformation, 1);
            return cubes;
        }
        JsonArray result = new JsonArray();
        Matrix4f local = BedrockModelTransform.bedrockMatrix(transformation, 1);
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            Vector3f origin = VectorUtils.vector3f(cube.getAsJsonArray("origin"));
            Vector3f size = VectorUtils.vector3f(cube.getAsJsonArray("size"));
            Matrix4f pose = new Matrix4f(local);
            if (cube.has("rotation")) {
                Vector3f pivot = VectorUtils.vector3f(cube.getAsJsonArray("pivot"));
                Vector3f rotation = VectorUtils.vector3f(cube.getAsJsonArray("rotation")).mul((float) Math.PI / 180);
                pose.translate(pivot).rotateZYX(-rotation.z, rotation.y, -rotation.x).translate(-pivot.x, -pivot.y, -pivot.z);
            }
            JsonObject transformed = transformCube(cube, origin, size, pose);
            if (transformed != null) {
                result.add(transformed);
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : cube.getAsJsonObject("uv").entrySet()) {
                Direction direction = Direction.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                Vector3f corner = pose.transformPosition(corner(origin, size, direction, 0, 0));
                Vector3f across = pose.transformPosition(corner(origin, size, direction, 1, 0)).sub(corner);
                Vector3f down = pose.transformPosition(corner(origin, size, direction, 0, 1)).sub(corner);
                float width = across.length();
                if (width == 0) {
                    continue;
                }
                across.div(width);
                float shear = down.dot(across);
                down.sub(new Vector3f(across).mul(shear));
                float height = down.length();
                // 面积为零的面不产生片元；只丢弃该面，不影响压扁后仍有面积的其他面。
                if (height <= 1e-6f * Math.max(width, Math.abs(shear))) {
                    continue;
                }
                down.div(height);
                JsonObject face = entry.getValue().getAsJsonObject().deepCopy();
                if (direction.axis() == Direction.Axis.Y) {
                    JsonArray uv = face.getAsJsonArray("uv");
                    JsonArray span = face.getAsJsonArray("uv_size");
                    face.add("uv", pair(uv.get(0).getAsFloat() + span.get(0).getAsFloat(), uv.get(1).getAsFloat() + span.get(1).getAsFloat()));
                    face.add("uv_size", pair(-span.get(0).getAsFloat(), -span.get(1).getAsFloat()));
                }
                float left = 0;
                float rectangleWidth = width;
                if (Math.abs(shear) > 1e-5f * Math.max(width, height)) {
                    left = Math.min(0, shear);
                    rectangleWidth += Math.abs(shear);
                    String material = face.get("material_instance").getAsString();
                    // 用平面矩形承载错切面，透明像素裁出平行四边形；按原贴图的像素密度采样。
                    Bake key = new Bake(
                            materials.get(material).data(),
                            face.getAsJsonArray("uv").deepCopy(),
                            face.getAsJsonArray("uv_size").deepCopy(),
                            face.has("uv_rotation") ? face.get("uv_rotation").getAsInt() : 0,
                            Math.round(shear / width * 1_000_000f) / 1_000_000f
                    );
                    BakedTexture cached = this.textures.get(key);
                    if (cached == null) {
                        cached = new BakedTexture(this.textures.size(), bake(key));
                        this.textures.put(key, cached);
                    }
                    TextureData baked = cached.data();
                    String slot = material + "_affine_" + cached.index();
                    materials.put(slot, new BedrockItemGeometry.Material(baked, materials.get(material).color()));
                    face.addProperty("material_instance", slot);
                    int side = Math.max(baked.image().getWidth(), baked.image().getHeight());
                    face.add("uv", pair(0, 0));
                    face.add("uv_size", pair(16f * baked.image().getWidth() / side, 16f * baked.image().getHeight() / side));
                    face.remove("uv_rotation");
                }
                Vector3f center = new Vector3f(corner).fma(left + rectangleWidth / 2, across).fma(height / 2, down);
                Matrix3d rotation = new Matrix3d().setColumn(0, -across.x, -across.y, -across.z)
                        .setColumn(1, -down.x, -down.y, -down.z);
                Vector3f normal = new Vector3f(across).cross(down);
                rotation.setColumn(2, normal.x, normal.y, normal.z);
                JsonObject plane = new JsonObject();
                plane.add("origin", VectorUtils.toJson(new Vector3f(center).sub(rectangleWidth / 2, height / 2, 0)));
                plane.add("size", VectorUtils.toJson(rectangleWidth, height, 0));
                plane.add("pivot", VectorUtils.toJson(center));
                plane.add("rotation", VectorUtils.toJson(BedrockModelTransform.rotationAngles(rotation)));
                JsonObject faces = new JsonObject();
                faces.add("south", face);
                plane.add("uv", faces);
                result.add(plane);
            }
        }
        return result;
    }

    private static JsonObject transformCube(JsonObject cube, Vector3f origin, Vector3f size, Matrix4fc pose) {
        Vector3f x = new Vector3f(pose.m00(), pose.m01(), pose.m02());
        Vector3f y = new Vector3f(pose.m10(), pose.m11(), pose.m12());
        Vector3f z = new Vector3f(pose.m20(), pose.m21(), pose.m22());
        Vector3f scale = new Vector3f(x.length(), y.length(), z.length());
        if (scale.x == 0 || scale.y == 0 || scale.z == 0 || pose.determinant3x3() <= 0) {
            return null;
        }
        x.div(scale.x);
        y.div(scale.y);
        z.div(scale.z);
        if (Math.abs(x.dot(y)) > 1e-5f || Math.abs(x.dot(z)) > 1e-5f || Math.abs(y.dot(z)) > 1e-5f) {
            return null;
        }
        // 非等比缩放不一定造成错切；三个元素轴仍正交时保留整个 cube 和原 UV，避免拆成六个面。
        Vector3f center = pose.transformPosition(new Vector3f(size).mul(0.5f).add(origin));
        Vector3f transformedSize = new Vector3f(size).mul(scale);
        Matrix3d rotation = new Matrix3d().setColumn(0, x.x, x.y, x.z).setColumn(1, y.x, y.y, y.z).setColumn(2, z.x, z.y, z.z);
        JsonObject result = cube.deepCopy();
        result.add("origin", VectorUtils.toJson(new Vector3f(center).sub(new Vector3f(transformedSize).mul(0.5f))));
        result.add("size", VectorUtils.toJson(transformedSize));
        result.add("pivot", VectorUtils.toJson(center));
        result.add("rotation", VectorUtils.toJson(BedrockModelTransform.rotationAngles(rotation)));
        return result;
    }

    private static Vector3f corner(Vector3f origin, Vector3f size, Direction face, int u, int v) {
        Vector3f point = switch (face) {
            case SOUTH -> new Vector3f(1 - u, 1 - v, 1);
            case NORTH -> new Vector3f(u, 1 - v, 0);
            case EAST -> new Vector3f(0, 1 - v, 1 - u);
            case WEST -> new Vector3f(1, 1 - v, u);
            case UP -> new Vector3f(1 - u, 1, v);
            case DOWN -> new Vector3f(1 - u, 0, 1 - v);
        };
        return point.mul(size).add(origin);
    }

    private static JsonArray pair(float first, float second) {
        JsonArray result = new JsonArray();
        result.add(first);
        result.add(second);
        return result;
    }

    private static TextureData bake(Bake key) throws IOException {
        BufferedImage source = key.texture().image();
        int side = Math.max(source.getWidth(), source.getHeight());
        double pixelsU = Math.abs(key.span().get(0).getAsDouble()) * side / 16;
        double pixelsV = Math.abs(key.span().get(1).getAsDouble()) * side / 16;
        boolean turned = key.rotation() == 90 || key.rotation() == 270;
        int width = Math.max(1, (int) Math.ceil((turned ? pixelsV : pixelsU) * (1 + Math.abs(key.shear()))));
        int height = Math.max(1, (int) Math.ceil(turned ? pixelsU : pixelsV));
        TextureAnimation animation = key.texture().animation();
        if (animation == null) {
            return new TextureData(bakeFrame(source, key, width, height), null);
        }
        int interval = animation.sampleInterval();
        int count = animation.duration() / interval;
        BufferedImage sheet = new BufferedImage(width, Math.multiplyExact(height, count), BufferedImage.TYPE_INT_ARGB);
        List<TextureAnimation.Frame> frames = new ArrayList<>(count);
        key.texture().forEachFrame(0xffffff, (index, frame) -> {
            BufferedImage baked = bakeFrame(frame, key, width, height);
            sheet.setRGB(0, index * height, width, height, baked.getRGB(0, 0, width, height, null, 0, width), 0, width);
            frames.add(new TextureAnimation.Frame(index, interval));
        });
        TextureAnimation converted = new TextureAnimation(sheet, width, height, frames, false, false, animation.duration());
        return new TextureData(converted.firstFrame(), converted);
    }

    private static BufferedImage bakeFrame(BufferedImage source, Bake key, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int side = Math.max(source.getWidth(), source.getHeight());
        for (int y = 0; y < height; y++) {
            double v = (y + 0.5) / height;
            for (int x = 0; x < width; x++) {
                double u = Math.min(0, key.shear()) + (x + 0.5) / width * (1 + Math.abs(key.shear())) - v * key.shear();
                if (u < 0 || u >= 1) {
                    continue;
                }
                double textureU = switch (key.rotation()) {
                    case 90 -> v;
                    case 180 -> 1 - u;
                    case 270 -> 1 - v;
                    default -> u;
                };
                double textureV = switch (key.rotation()) {
                    case 90 -> 1 - u;
                    case 180 -> 1 - v;
                    case 270 -> u;
                    default -> v;
                };
                int pixelX = (int) Math.floor((key.uv().get(0).getAsDouble() + textureU * key.span().get(0).getAsDouble()) * side / 16);
                int pixelY = (int) Math.floor((key.uv().get(1).getAsDouble() + textureV * key.span().get(1).getAsDouble()) * side / 16);
                image.setRGB(x, y, source.getRGB(Math.floorMod(pixelX, source.getWidth()), Math.floorMod(pixelY, source.getHeight())));
            }
        }
        return image;
    }

    private record Bake(TextureData texture, JsonArray uv, JsonArray span, int rotation, float shear) {
    }

    private record BakedTexture(int index, TextureData data) {
    }
}
