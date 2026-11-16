package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.util.MatrixDecomposition;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.*;

import java.lang.Math;
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

    public static Matrix4fc relativeDisplay(JavaModel.Transform reference, JavaModel.Transform target) {
        if (reference.equals(target)) {
            return IDENTITY;
        }
        // 公共 display 在外层应用，因此部件只需烘焙 reference⁻¹ × target。
        // 两侧的 ±0.5 将居中 display 坐标转回 0..1 的模型坐标，再与部件局部矩阵相乘。
        Matrix4f relative = new Matrix4f().translation(0.5f, 0.5f, 0.5f)
                .mul(displayMatrix(reference).invert())
                .mul(displayMatrix(target))
                .translate(-0.5f, -0.5f, -0.5f);
        validate(relative);
        // 消除公共变换求逆产生的浮点尾差，使相同的部件相对姿态命中同一份几何。
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                relative.set(column, row, (float) (Math.rint(relative.get(column, row) * 1_000_000.0) / 1_000_000.0));
            }
        }
        return relative;
    }

    private static Matrix4f displayMatrix(JavaModel.Transform transform) {
        return new Matrix4f().translation(new Vector3f(transform.translation()).div(16))
                .rotateXYZ(transform.rotation().x * RADIANS, transform.rotation().y * RADIANS, transform.rotation().z * RADIANS)
                .scale(transform.scale());
    }

    public static float uniformScale(Matrix4fc matrix) {
        validate(matrix);
        if (!canBakeCubes(matrix)) {
            throw new BedrockResourceException("resource_pack.bedrock.reason.nonuniform_transform");
        }
        return new Vector3f(matrix.m00(), matrix.m01(), matrix.m02()).length();
    }

    public static void validate(Matrix4fc matrix) {
        if (!matrix.isFinite() || matrix.m03() != 0 || matrix.m13() != 0 || matrix.m23() != 0 || matrix.m33() != 1) {
            throw new BedrockResourceException("resource_pack.bedrock.reason.affine_transform");
        }
    }

    public static boolean canBakeCubes(Matrix4fc matrix) {
        if (!matrix.isFinite() || matrix.m03() != 0 || matrix.m13() != 0 || matrix.m23() != 0 || matrix.m33() != 1) {
            return false;
        }
        // 等长且两两垂直的列向量才可直接烘焙所有 cube；其他仿射矩阵交给骨骼或逐面转换。
        Vector3f x = new Vector3f(matrix.m00(), matrix.m01(), matrix.m02());
        Vector3f y = new Vector3f(matrix.m10(), matrix.m11(), matrix.m12());
        Vector3f z = new Vector3f(matrix.m20(), matrix.m21(), matrix.m22());
        float squared = x.lengthSquared();
        float tolerance = squared * 0.00001f;
        return squared > 0 && matrix.determinant3x3() > 0
                && Math.abs(y.lengthSquared() - squared) <= tolerance && Math.abs(z.lengthSquared() - squared) <= tolerance
                && Math.abs(x.dot(y)) <= tolerance && Math.abs(x.dot(z)) <= tolerance && Math.abs(y.dot(z)) <= tolerance;
    }

    public static Matrix4f bedrockMatrix(Matrix4fc transformation, float geometryScale) {
        validate(transformation);
        Matrix4f coordinates = new Matrix4f().translation(8 * geometryScale, 8 * (1 - geometryScale), -8 * geometryScale)
                .scale(-16 * geometryScale, 16 * geometryScale, 16 * geometryScale);
        return new Matrix4f(coordinates).mul(transformation).mul(coordinates.invert());
    }

    public static void addBones(JsonArray bones, JsonObject channels, String parent, String prefix, JsonArray cubes, Matrix4fc matrix) {
        MatrixDecomposition.Result parts = MatrixDecomposition.decompose(new Matrix3d(matrix));
        // T·U·S·Vᵀ：每层只承担一种变换，明确固定次序，不依赖单骨骼内各动画通道的组合次序。
        // 全部以零点为支点，避免父子 pivot 的补偿再次引入平移。
        String[] suffixes = {"translation", "left", "scale", "right"};
        String[] properties = {"position", "rotation", "scale", "rotation"};
        Vector3f[] values = {new Vector3f(matrix.m30(), matrix.m31(), matrix.m32()), rotationAngles(parts.left()),
                new Vector3f(parts.scale()), rotationAngles(parts.right())};
        for (int index = 0; index < suffixes.length; index++) {
            String name = prefix + "affine_" + suffixes[index];
            JsonObject bone = new JsonObject();
            bone.addProperty("name", name);
            bone.addProperty("parent", parent);
            bone.add("pivot", VectorUtils.toJson(0, 0, 0));
            if (index == suffixes.length - 1) {
                bone.add("cubes", cubes);
            }
            bones.add(bone);
            JsonObject channel = new JsonObject();
            channel.add(properties[index], VectorUtils.toJson(values[index]));
            channels.add(name, channel);
            parent = name;
        }
    }

    public static void apply(JsonArray cubes, Matrix4fc transformation, float geometryScale) {
        if (isIdentity(transformation)) {
            return;
        }
        float scale = uniformScale(transformation);
        // Java 在 display 居中之后乘入局部矩阵，局部坐标仍为 0..1；同时还原已有的几何缩小。
        // C × M × C⁻¹ 将 Java 局部变换换算到当前基岩几何坐标，不能把两套坐标下的矩阵直接相乘。
        Matrix4f matrix = bedrockMatrix(transformation, geometryScale);
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf());
        Matrix3d cubeRotation = new Matrix3d();
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
            Vector3f angles = rotationAngles(cubeRotation.rotation(rotation.mul(local, local)));
            cube.add("origin", VectorUtils.toJson(new Vector3f(center).sub(new Vector3f(size).mul(0.5f))));
            cube.add("size", VectorUtils.toJson(size));
            cube.add("pivot", VectorUtils.toJson(center));
            cube.add("rotation", VectorUtils.toJson(angles));
        }
    }

    public static Vector3f rotationAngles(Matrix3dc rotation) {
        double cosY = Math.hypot(rotation.m00(), rotation.m01());
        double y = Math.atan2(-rotation.m02(), cosY);
        double x;
        double z;
        if (cosY < 1e-6) {
            // Y 为 ±90° 时 X、Z 轴重合。固定 Z 为零，从第二列和第三列保留两者合成的旋转。
            x = Math.atan2(-rotation.m21(), rotation.m11());
            z = 0;
        } else {
            x = Math.atan2(rotation.m12(), rotation.m22());
            z = Math.atan2(rotation.m01(), rotation.m00());
        }
        return new Vector3f((float) -Math.toDegrees(x), (float) Math.toDegrees(y), (float) -Math.toDegrees(z));
    }
}
