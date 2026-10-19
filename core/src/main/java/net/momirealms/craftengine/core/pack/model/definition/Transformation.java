package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @param matrix 一个不可变的列表，其中包含16个浮点数元素，描述一个行主序（Row-major）矩阵
 */
public record Transformation(List<Float> matrix) {
    private static final String[] RIGHT_ROTATION = ConfigKeys.of("right_rotation");
    private static final String[] LEFT_ROTATION = ConfigKeys.of("left_rotation");

    public Transformation {
        matrix = List.copyOf(matrix);
        if (matrix.size() != 16) {
            throw new IllegalArgumentException("Transformation requires 16 matrix values");
        }
    }

    public Matrix4f toMatrix() {
        return new Matrix4f(
                this.matrix.get(0), this.matrix.get(4), this.matrix.get(8), this.matrix.get(12),
                this.matrix.get(1), this.matrix.get(5), this.matrix.get(9), this.matrix.get(13),
                this.matrix.get(2), this.matrix.get(6), this.matrix.get(10), this.matrix.get(14),
                this.matrix.get(3), this.matrix.get(7), this.matrix.get(11), this.matrix.get(15));
    }

    public Transformation(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
        this(toMatrix(translation, leftRotation, scale, rightRotation));
    }

    public static Transformation fromConfig(ConfigValue value) {
        if (value.is(List.class)) {
            List<Object> rawList = value.getAsList();
            if (rawList.size() == 4 && rawList.getFirst() instanceof String) {
                List<Float> matrix = new java.util.ArrayList<>(16);
                for (int i = 0; i < 4; i++) {
                    String row = rawList.get(i).toString().replace("_", "");
                    String[] parts = row.split(",");
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("Each row of transformation matrix must have exactly 4 values, got " + parts.length + " in row " + i);
                    }
                    for (String part : parts) {
                        matrix.add(Float.parseFloat(part.trim()));
                    }
                }
                return new Transformation(Collections.unmodifiableList(matrix));
            }
            return new Transformation(Collections.unmodifiableList(value.getAsFixedSizeList(16, ConfigValue::getAsFloat)));
        }
        ConfigSection section = value.getAsSection();
        Quaternionf rightRotation = section.getValue(RIGHT_ROTATION, Transformation::parseRotation, ConfigConstants.ZERO_QUATERNION);
        Quaternionf leftRotation = section.getValue(LEFT_ROTATION, Transformation::parseRotation, ConfigConstants.ZERO_QUATERNION);
        Vector3f scale = section.getVector3f(ModelJsonKeys.SCALE, ConfigConstants.NORMAL_SCALE);
        Vector3f translation = section.getVector3f(ModelJsonKeys.TRANSLATION, ConfigConstants.ZERO_VECTOR3);
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }

    public static Transformation fromJson(JsonElement json) {
        if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            if (!jsonArray.isEmpty() && jsonArray.get(0).isJsonArray()) {
                if (jsonArray.size() != 4) {
                    throw new IllegalArgumentException("Invalid transformation matrix: expected 4 rows, got " + jsonArray.size());
                }
                List<Float> matrix = new java.util.ArrayList<>(16);
                for (int i = 0; i < 4; i++) {
                    JsonArray row = jsonArray.get(i).getAsJsonArray();
                    if (row.size() != 4) {
                        throw new IllegalArgumentException("Invalid transformation matrix: each row must have 4 values, row " + i + " has " + row.size());
                    }
                    for (int j = 0; j < 4; j++) {
                        matrix.add(row.get(j).getAsFloat());
                    }
                }
                return new Transformation(matrix);
            }
            List<Float> list = jsonArray.asList().stream().map(JsonElement::getAsFloat).toList();
            if (list.size() != 16) {
                throw new IllegalArgumentException("Invalid transformation matrix");
            }
            return new Transformation(list);
        }
        JsonObject jsonObject = json.getAsJsonObject();
        Quaternionf rightRotation = jsonObject.has("right_rotation") ? parseRotation(jsonObject.get("right_rotation")) : new Quaternionf();
        Quaternionf leftRotation = jsonObject.has("left_rotation") ? parseRotation(jsonObject.get("left_rotation")) : new Quaternionf();
        JsonArray scaleArray = jsonObject.getAsJsonArray(ModelJsonKeys.SCALE);
        Vector3f scale = scaleArray == null ? new Vector3f(1) : new Vector3f(scaleArray.get(0).getAsFloat(), scaleArray.get(1).getAsFloat(), scaleArray.get(2).getAsFloat());
        JsonArray translationArray = jsonObject.getAsJsonArray(ModelJsonKeys.TRANSLATION);
        Vector3f translation = translationArray == null ? new Vector3f() : new Vector3f(translationArray.get(0).getAsFloat(), translationArray.get(1).getAsFloat(), translationArray.get(2).getAsFloat());
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
            float angle = section.getFloat(ModelJsonKeys.ANGLE);
            Vector3f axis = section.getVector3f(ModelJsonKeys.AXIS, ConfigConstants.ZERO_VECTOR3);
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
        float angle = rotation.get(ModelJsonKeys.ANGLE).getAsFloat();
        JsonArray axis = rotation.getAsJsonArray(ModelJsonKeys.AXIS);
        return new Quaternionf(new AxisAngle4f(angle, axis.get(0).getAsFloat(), axis.get(1).getAsFloat(), axis.get(2).getAsFloat()));
    }

    private static List<Float> toMatrix(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
        // Java composes T * leftRotation * scale * rightRotation; rightRotation is not transposed.
        Matrix4f matrix = new Matrix4f().translation(translation).rotate(leftRotation).scale(scale).rotate(rightRotation);
        return List.of(
                matrix.m00(), matrix.m10(), matrix.m20(), matrix.m30(),
                matrix.m01(), matrix.m11(), matrix.m21(), matrix.m31(),
                matrix.m02(), matrix.m12(), matrix.m22(), matrix.m32(),
                matrix.m03(), matrix.m13(), matrix.m23(), matrix.m33());
    }
}
