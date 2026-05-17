package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @param matrix 一个不可变的列表，其中包含16个浮点数元素，描述一个行主序（Row-major）矩阵
 */
public record Transformation(List<Float> matrix) {
    private static final String[] RIGHT_ROTATION = new String[]{"right_rotation", "right-rotation"};
    private static final String[] LEFT_ROTATION = new String[]{"left_rotation", "left-rotation"};

    public Transformation(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
        this(toMatrix(translation, leftRotation, scale, rightRotation));
    }

    public static Transformation fromConfig(ConfigValue value) {
        if (value.is(List.class)) {
            return new Transformation(Collections.unmodifiableList(value.getAsFixedSizeList(16, ConfigValue::getAsFloat)));
        }
        ConfigSection section = value.getAsSection();
        Quaternionf rightRotation = section.getValue(RIGHT_ROTATION, Transformation::parseRotation, ConfigConstants.ZERO_QUATERNION);
        Quaternionf leftRotation = section.getValue(LEFT_ROTATION, Transformation::parseRotation, ConfigConstants.ZERO_QUATERNION);
        Vector3f scale = section.getVector3f("scale", ConfigConstants.NORMAL_SCALE);
        Vector3f translation = section.getVector3f("translation", ConfigConstants.ZERO_VECTOR3);
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }

    public static Transformation fromJson(JsonElement json) {
        if (json.isJsonArray()) {
            List<Float> list = json.getAsJsonArray().asList().stream().map(JsonElement::getAsFloat).toList();
            if (list.size() != 16) throw new IllegalArgumentException("Invalid transformation matrix");
            return new Transformation(list);
        }
        JsonObject jsonObject = json.getAsJsonObject();
        Quaternionf rightRotation = parseRotation(jsonObject.get("right_rotation"));
        Quaternionf leftRotation = parseRotation(jsonObject.get("left_rotation"));
        JsonArray scaleArray = jsonObject.getAsJsonArray("scale");
        Vector3f scale = new Vector3f(scaleArray.get(0).getAsFloat(), scaleArray.get(1).getAsFloat(), scaleArray.get(2).getAsFloat());
        JsonArray translationArray = jsonObject.getAsJsonArray("translation");
        Vector3f translation = new Vector3f(translationArray.get(0).getAsFloat(), translationArray.get(1).getAsFloat(), translationArray.get(2).getAsFloat());
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }

    public JsonElement toJson() {
        JsonArray jsonArray = new JsonArray(16);
        for (int i = 0; i < 16; i++) {
            jsonArray.add(this.matrix.get(i));
        }
        return jsonArray;
    }

    private static Quaternionf parseRotation(ConfigValue value) {
        if (value.is(Map.class)) {
            ConfigSection section = value.getAsSection();
            float angle = section.getFloat("angle");
            Vector3f axis = section.getVector3f("axis", ConfigConstants.ZERO_VECTOR3);
            return new Quaternionf(new AxisAngle4f(angle, axis.x, axis.y, axis.z));
        }
        return value.getAsQuaternion();
    }

    private static Quaternionf parseRotation(JsonElement json) {
        if (json.isJsonArray()) {
            JsonArray rotationArray = json.getAsJsonArray();
            return new Quaternionf(rotationArray.get(0).getAsFloat(), rotationArray.get(1).getAsFloat(), rotationArray.get(2).getAsFloat(), rotationArray.get(3).getAsFloat());
        }
        JsonObject rotation = json.getAsJsonObject();
        float angle = rotation.get("angle").getAsFloat();
        JsonArray axis = rotation.getAsJsonArray("axis");
        return new Quaternionf(new AxisAngle4f(angle, axis.get(0).getAsFloat(), axis.get(1).getAsFloat(), axis.get(2).getAsFloat()));
    }

    private static List<Float> toMatrix(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
        Matrix3f V = new Matrix3f().set(rightRotation); // 初始旋转矩阵 V
        Matrix3f S = new Matrix3f().scaling(scale);     // 缩放对角矩阵 S
        Matrix3f U = new Matrix3f().set(leftRotation);  // 再次旋转矩阵 U

        // M = U * S * V^T（SVD重建）
        Matrix3f VT = new Matrix3f(V).transpose();
        Matrix3f M = new Matrix3f(U).mul(S).mul(VT);

        // 构建行主序16个元素的列表
        return List.of(
                // row 0
                M.m00(), M.m10(), M.m20(), translation.x,
                // row 1
                M.m01(), M.m11(), M.m21(), translation.y,
                // row 2
                M.m02(), M.m12(), M.m22(), translation.z,
                // row 3（第13/14/15无效，第16为scale因子=1）
                0f, 0f, 0f, 1f
        );
    }
}
