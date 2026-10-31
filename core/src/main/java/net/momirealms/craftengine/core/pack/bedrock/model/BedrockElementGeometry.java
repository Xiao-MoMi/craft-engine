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

    public static float fittingScale(JsonArray cubes) {
        if (cubes.isEmpty()) {
            return 1;
        }
        Vector3f minimum = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f maximum = new Vector3f(Float.NEGATIVE_INFINITY);
        bounds(cubes, minimum, maximum);
        return Math.min(horizontalScale(minimum, maximum), centeredScale(minimum.y, maximum.y));
    }

    private static void bounds(JsonArray cubes, Vector3f minimum, Vector3f maximum) {
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
    }

    private static float horizontalScale(Vector3f minimum, Vector3f maximum) {
        return Math.min(centeredScale(minimum.x, maximum.x), centeredScale(minimum.z, maximum.z));
    }

    private static float centeredScale(float minimum, float maximum) {
        float min = minimum - 8;
        float max = maximum - 8;
        float scale = 1;
        if (max - min > MAX_GEOMETRY_SIZE - GEOMETRY_MARGIN) {
            scale = (MAX_GEOMETRY_SIZE - GEOMETRY_MARGIN) / (max - min);
        }
        if (min < 0) {
            scale = Math.min(scale, (MAX_CENTER_DISTANCE - GEOMETRY_MARGIN) / -min);
        }
        if (max > 0) {
            scale = Math.min(scale, (MAX_CENTER_DISTANCE - GEOMETRY_MARGIN) / max);
        }
        // 偏离中心的模型至少伸入标准方块一个单位。
        if (min > 0) {
            scale = Math.min(scale, (7 - GEOMETRY_MARGIN) / min);
        } else if (max < 0) {
            scale = Math.min(scale, (7 - GEOMETRY_MARGIN) / -max);
        }
        return scale;
    }

    public static Result fit(JsonArray cubes) {
        float scale = fittingScale(cubes);
        scaleCubes(cubes, scale, 8);
        return new Result(cubes, scale);
    }

    public static float fitWorldBlock(JsonArray cubes) {
        if (cubes.isEmpty()) {
            return 1;
        }
        Vector3f minimum = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f maximum = new Vector3f(Float.NEGATIVE_INFINITY);
        bounds(cubes, minimum, maximum);
        if (fitsAxis(minimum.x, maximum.x) && fitsAxis(minimum.y, maximum.y) && fitsAxis(minimum.z, maximum.z)) {
            return 1;
        }
        float horizontal = horizontalScale(minimum, maximum);
        float pivotY = 8;
        float scale = Math.min(horizontal, centeredScale(minimum.y, maximum.y));
        if (minimum.y == 0 && maximum.y > 0) {
            // 贴地模型绕底部缩放；缩小后仍须满足客户端的方块重叠条件。
            float top = Math.min(MAX_GEOMETRY_SIZE, 8 + MAX_CENTER_DISTANCE);
            float grounded = Math.min(horizontal, (top - GEOMETRY_MARGIN) / maximum.y);
            if (grounded * maximum.y >= 1) {
                pivotY = 0;
                scale = grounded;
            }
        }
        scaleCubes(cubes, scale, pivotY);
        return scale;
    }

    private static boolean fitsAxis(float min, float max) {
        return max - min <= MAX_GEOMETRY_SIZE && min >= 8 - MAX_CENTER_DISTANCE && max <= 8 + MAX_CENTER_DISTANCE
                && min <= 15 && max >= 1;
    }

    private static void scaleCubes(JsonArray cubes, float scale, float pivotY) {
        if (scale == 1) {
            return;
        }
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            scale(cube.getAsJsonArray("origin"), scale, pivotY);
            scale(cube.getAsJsonArray("size"), scale, 0);
            if (cube.has("pivot")) {
                scale(cube.getAsJsonArray("pivot"), scale, pivotY);
            }
        }
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

    private static void scale(JsonArray vector, float scale, float pivotY) {
        for (int axis = 0; axis < 3; axis++) {
            float center = axis == 1 ? pivotY : 0;
            vector.set(axis, new com.google.gson.JsonPrimitive((vector.get(axis).getAsFloat() - center) * scale + center));
        }
    }

    public record Material(String name, float uScale, float vScale) {
    }

    public record Result(JsonArray cubes, float scale) {
    }
}
