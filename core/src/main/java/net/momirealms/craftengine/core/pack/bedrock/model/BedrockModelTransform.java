package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class BedrockModelTransform {
    private static final float RADIANS = (float) (Math.PI / 180);
    private static final Matrix4fc IDENTITY = new Matrix4f();

    private BedrockModelTransform() {
    }

    public static Matrix4fc compose(List<Transformation> transformations) {
        if (transformations.isEmpty()) {
            return IDENTITY;
        }
        Matrix4f matrix = new Matrix4f();
        for (Transformation transformation : transformations) {
            matrix.mul(transformation.toMatrix());
        }
        return matrix;
    }

    public static boolean isIdentity(Matrix4fc matrix) {
        return matrix.equals(IDENTITY, 0.000001f);
    }

    public static float uniformScale(Matrix4fc matrix) {
        Vector3f x = new Vector3f(matrix.m00(), matrix.m01(), matrix.m02());
        Vector3f y = new Vector3f(matrix.m10(), matrix.m11(), matrix.m12());
        Vector3f z = new Vector3f(matrix.m20(), matrix.m21(), matrix.m22());
        float scale = x.length();
        if (!matrix.isFinite() || matrix.m03() != 0 || matrix.m13() != 0 || matrix.m23() != 0 || matrix.m33() != 1
                || scale <= 0 || matrix.determinant3x3() <= 0) {
            throw new UnsupportedOperationException("Model transformation requires an affine matrix with positive uniform scale");
        }
        x.div(scale);
        y.div(scale);
        z.div(scale);
        if (Math.abs(y.lengthSquared() - 1) > 0.00001f || Math.abs(z.lengthSquared() - 1) > 0.00001f
                || Math.abs(x.dot(y)) > 0.00001f || Math.abs(x.dot(z)) > 0.00001f || Math.abs(y.dot(z)) > 0.00001f) {
            throw new UnsupportedOperationException("Non-uniform scale and shear in model transformations are not supported");
        }
        return scale;
    }

    public static void apply(JsonArray cubes, Matrix4fc transformation, float geometryScale) {
        if (isIdentity(transformation)) {
            return;
        }
        float scale = uniformScale(transformation);
        // Java 在 display 居中之后乘入局部矩阵，局部坐标仍为 0..1；同时还原已有的几何缩小。
        Matrix4f coordinates = new Matrix4f().translation(8 * geometryScale, 8 * (1 - geometryScale), -8 * geometryScale)
                .scale(-16 * geometryScale, 16 * geometryScale, 16 * geometryScale);
        Matrix4f matrix = new Matrix4f(coordinates).mul(transformation).mul(new Matrix4f(coordinates).invert());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf());
        for (JsonElement value : cubes) {
            JsonObject cube = value.getAsJsonObject();
            Vector3f origin = VectorUtils.vector3f(cube.getAsJsonArray("origin"));
            Vector3f size = VectorUtils.vector3f(cube.getAsJsonArray("size"));
            Vector3f center = new Vector3f(size).mul(0.5f).add(origin);
            Quaternionf local = new Quaternionf();
            if (cube.has("rotation")) {
                Vector3f angles = VectorUtils.vector3f(cube.getAsJsonArray("rotation")).mul(RADIANS);
                // 基岩 JSON 的 X/Z 旋转方向与这里的几何坐标相反。
                local.rotationZYX(-angles.z, angles.y, -angles.x);
                Vector3f pivot = VectorUtils.vector3f(cube.getAsJsonArray("pivot"));
                local.transform(center.sub(pivot)).add(pivot);
            }
            matrix.transformPosition(center);
            size.mul(scale);
            Vector3f angles = new Quaternionf(rotation).mul(local).getEulerAnglesZYX(new Vector3f()).div(RADIANS).mul(-1, 1, -1);
            cube.add("origin", VectorUtils.toJson(new Vector3f(center).sub(new Vector3f(size).mul(0.5f))));
            cube.add("size", VectorUtils.toJson(size));
            cube.add("pivot", VectorUtils.toJson(center));
            cube.add("rotation", VectorUtils.toJson(angles));
        }
    }
}
