package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Map;

public final class BedrockElementGeometry {
    private static final float RADIANS = (float) (Math.PI / 180);
    private static final float MAX_GEOMETRY_SIZE = 30;
    private static final float MAX_CENTER_DISTANCE = 22;
    private static final float GEOMETRY_MARGIN = 0.0001f;

    private BedrockElementGeometry() {
    }

    public static JsonArray convert(JavaModel model, Map<JavaModel.Face, Material> materials) {
        JsonArray cubes = new JsonArray();
        for (JavaModel.Element element : model.elements()) {
            if (element.faces().isEmpty()) {
                continue;
            }
            Vector3f from = new Vector3f(element.from());
            Vector3f to = new Vector3f(element.to());
            JavaModel.Rotation rotation = element.rotation();
            Matrix3f matrix = new Matrix3f();
            if (rotation != null) {
                Vector3f angles = rotation.angles();
                matrix.rotationZYX(angles.z * RADIANS, angles.y * RADIANS, angles.x * RADIANS);
                if (rotation.rescale()) {
                    // Java 按旋转矩阵各列的最大绝对分量补偿，多轴旋转不能相乘单轴缩放值。
                    Vector3f scale = new Vector3f(
                            1 / Math.max(Math.abs(matrix.m00()), Math.max(Math.abs(matrix.m01()), Math.abs(matrix.m02()))),
                            1 / Math.max(Math.abs(matrix.m10()), Math.max(Math.abs(matrix.m11()), Math.abs(matrix.m12()))),
                            1 / Math.max(Math.abs(matrix.m20()), Math.max(Math.abs(matrix.m21()), Math.abs(matrix.m22())))
                    );
                    from.sub(rotation.origin()).mul(scale).add(rotation.origin());
                    to.sub(rotation.origin()).mul(scale).add(rotation.origin());
                }
            }
            Vector3f origin = new Vector3f(8 - to.x, from.y, from.z - 8);
            Vector3f size = new Vector3f(to).sub(from);
            JsonObject faces = new JsonObject();
            boolean signed = size.x < 0 || size.y < 0 || size.z < 0;
            for (Map.Entry<Direction, JavaModel.Face> entry : element.faces().entrySet()) {
                Direction direction = entry.getKey();
                JavaModel.Face source = entry.getValue();
                Material material = materials.get(source);
                JavaModel.Uv uv = source.uv();
                boolean vertical = direction.axis() == Direction.Axis.Y;
                JsonObject face = new JsonObject();
                face.add("uv", VectorUtils.toJson((
                        vertical ? uv.u2() : uv.u1()) * material.uScale(),
                        (vertical ? uv.v2() : uv.v1()) * material.vScale()
                ));
                face.add("uv_size", VectorUtils.toJson((
                        vertical ? uv.u1() - uv.u2() : uv.u2() - uv.u1()) * material.uScale(),
                        (vertical ? uv.v1() - uv.v2() : uv.v2() - uv.v1()) * material.vScale()
                ));
                face.addProperty("material_instance", material.name());
                int uvRotation = source.rotation();
                if (signed) {
                    int axis = switch (direction.axis()) {
                        case X -> 0;
                        case Y -> 1;
                        case Z -> 2;
                    };
                    float horizontal = axis == 0 ? size.z : size.x;
                    float height = axis == 1 ? size.z : size.y;
                    if (horizontal == 0 || height == 0) {
                        continue;
                    }
                    // 负尺寸拆为有朝向的面，直接取绝对值会反转面剔除与 UV 顶点顺序。
                    Direction facing = (horizontal < 0) != (height < 0) ? direction.opposite() : direction;
                    Vector3f planeOrigin = new Vector3f(origin).add(Math.min(size.x, 0), Math.min(size.y, 0), Math.min(size.z, 0));
                    planeOrigin.setComponent(axis, switch (direction) {
                        case WEST -> origin.x + size.x;
                        case EAST -> origin.x;
                        case UP -> origin.y + size.y;
                        case DOWN -> origin.y;
                        case SOUTH -> origin.z + size.z;
                        case NORTH -> origin.z;
                    });
                    Vector3f planeSize = new Vector3f(size).absolute().setComponent(axis, 0);
                    if (axis == 1 ? size.x < 0 : size.y < 0) {
                        uvRotation = (uvRotation + 180) % 360;
                    }
                    face.addProperty("uv_rotation", uvRotation);
                    JsonObject planeFaces = new JsonObject();
                    planeFaces.add(facing.name().toLowerCase(Locale.ROOT), face);
                    cubes.add(cube(planeOrigin, planeSize, rotation, planeFaces));
                } else {
                    if (uvRotation != 0) {
                        face.addProperty("uv_rotation", uvRotation);
                    }
                    faces.add(direction.name().toLowerCase(Locale.ROOT), face);
                }
            }
            if (!signed) {
                cubes.add(cube(origin, size, rotation, faces));
            }
        }
        return cubes;
    }

    public static Result fit(JsonArray cubes) {
        Vector3f minimum = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f maximum = new Vector3f(Float.NEGATIVE_INFINITY);
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            Vector3f origin = VectorUtils.vector3f(cube.getAsJsonArray("origin"));
            Vector3f size = VectorUtils.vector3f(cube.getAsJsonArray("size"));
            Vector3f pivot = new Vector3f();
            Matrix3f rotation = new Matrix3f();
            if (cube.has("rotation")) {
                pivot.set(VectorUtils.vector3f(cube.getAsJsonArray("pivot")));
                Vector3f angles = VectorUtils.vector3f(cube.getAsJsonArray("rotation")).mul(RADIANS);
                rotation.rotationZYX(-angles.z, angles.y, -angles.x);
            }
            Vector3f point = new Vector3f();
            for (int corner = 0; corner < 8; corner++) {
                point.set(origin).add((corner & 1) == 0 ? 0 : size.x, (corner & 2) == 0 ? 0 : size.y, (corner & 4) == 0 ? 0 : size.z);
                rotation.transform(point.sub(pivot)).add(pivot);
                point.set(8 - point.x, point.y, point.z + 8);
                minimum.min(point);
                maximum.max(point);
            }
        }
        return fit(cubes, minimum, maximum);
    }

    private static Result fit(JsonArray cubes, Vector3f minimum, Vector3f maximum) {
        float scale = 1;
        if (!cubes.isEmpty()) {
            for (int axis = 0; axis < 3; axis++) {
                float min = minimum.get(axis) - 8;
                float max = maximum.get(axis) - 8;
                float extent = max - min;
                // 预留浮点余量，避免坐标写出后重算边界略微超限，导致客户端拒绝整个模型。
                if (extent > MAX_GEOMETRY_SIZE - GEOMETRY_MARGIN) {
                    scale = Math.min(scale, (MAX_GEOMETRY_SIZE - GEOMETRY_MARGIN) / extent);
                }
                // 客户端边界为方块坐标 [-0.875, 1.875]，各轴距方块中心最多 22 个模型单位。
                if (min < 0) {
                    scale = Math.min(scale, (MAX_CENTER_DISTANCE - GEOMETRY_MARGIN) / -min);
                }
                if (max > 0) {
                    scale = Math.min(scale, (MAX_CENTER_DISTANCE - GEOMETRY_MARGIN) / max);
                }
                // 偏离中心的模型至少伸入标准方块一个单位，不能只满足整体尺寸限制。
                if (min > 0) {
                    scale = Math.min(scale, (7 - GEOMETRY_MARGIN) / min);
                } else if (max < 0) {
                    scale = Math.min(scale, (7 - GEOMETRY_MARGIN) / -max);
                }
            }
        }
        if (scale != 1) {
            // 几何围绕方块中心缩小，显示变换反向补偿，保留原有视觉尺寸。
            for (JsonElement value : cubes) {
                JsonObject cube = value.getAsJsonObject();
                scale(cube.getAsJsonArray("origin"), scale, true);
                scale(cube.getAsJsonArray("size"), scale, false);
                if (cube.has("pivot")) {
                    scale(cube.getAsJsonArray("pivot"), scale, true);
                }
            }
        }
        return new Result(cubes, scale);
    }

    private static JsonObject cube(Vector3f origin, Vector3f size, JavaModel.Rotation rotation, JsonObject faces) {
        JsonObject cube = new JsonObject();
        cube.add("origin", VectorUtils.toJson(origin));
        cube.add("size", VectorUtils.toJson(size));
        cube.add("uv", faces);
        if (rotation != null) {
            cube.add("pivot", VectorUtils.toJson(8 - rotation.origin().x, rotation.origin().y, rotation.origin().z - 8));
            cube.add("rotation", VectorUtils.toJson(-rotation.angles().x, -rotation.angles().y, rotation.angles().z));
        }
        return cube;
    }

    private static void scale(JsonArray vector, float scale, boolean position) {
        for (int axis = 0; axis < 3; axis++) {
            float center = position && axis == 1 ? 8 : 0;
            vector.set(axis, new com.google.gson.JsonPrimitive((vector.get(axis).getAsFloat() - center) * scale + center));
        }
    }

    public record Material(String name, float uScale, float vScale) {
    }

    public record Result(JsonArray cubes, float scale) {
    }
}
