package net.momirealms.craftengine.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.joml.Vector3fc;

import java.util.Map;

import static net.momirealms.craftengine.core.pack.model.ModelJsonKeys.*;

public final class ModelUtils {
    private static final double MIN_ELEMENT_COORDINATE = -16;
    private static final double MAX_ELEMENT_COORDINATE = 32;

    private ModelUtils() {
    }

    public static double[] offsetElements(JsonArray elements, float x, float y, float z) {
        double[] offset = {x, y, z};
        // Intersect the allowed translations of all elements before moving any of them.
        for (int axis = 0; axis < 3; axis++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (JsonElement entry : elements) {
                JsonObject element = entry.getAsJsonObject();
                double from = element.getAsJsonArray(FROM).get(axis).getAsDouble();
                double to = element.getAsJsonArray(TO).get(axis).getAsDouble();
                min = Math.min(min, Math.min(from, to));
                max = Math.max(max, Math.max(from, to));
            }
            offset[axis] = Math.clamp(offset[axis], MIN_ELEMENT_COORDINATE - min, MAX_ELEMENT_COORDINATE - max);
        }
        if (offset[0] == 0 && offset[1] == 0 && offset[2] == 0) {
            return offset;
        }
        for (JsonElement entry : elements) {
            JsonObject element = entry.getAsJsonObject();
            JsonArray from = element.getAsJsonArray(FROM);
            JsonArray to = element.getAsJsonArray(TO);
            // Implicit UVs depend on the original bounds. Fix them before translating the geometry.
            preserveDefaultUvs(element.getAsJsonObject(FACES), from, to);
            translate(from, offset);
            translate(to, offset);
            if (element.get(ROTATION) instanceof JsonObject rotation) {
                translate(rotation.getAsJsonArray(ORIGIN), offset);
            }
        }
        return offset;
    }

    private static void preserveDefaultUvs(JsonObject faces, JsonArray from, JsonArray to) {
        for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            if (face.has(UV)) {
                continue;
            }
            float[] uv = defaultUv(entry.getKey(),
                    from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                    to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat());
            if (uv != null) {
                JsonArray coordinates = new JsonArray(4);
                for (float coordinate : uv) {
                    coordinates.add(coordinate);
                }
                face.add(UV, coordinates);
            }
        }
    }

    private static void translate(JsonArray position, double[] offset) {
        for (int axis = 0; axis < 3; axis++) {
            if (offset[axis] != 0) {
                position.set(axis, new JsonPrimitive((float) (position.get(axis).getAsDouble() + offset[axis])));
            }
        }
    }

    public static float[] defaultUv(String face, float[] from, float[] to) {
        return defaultUv(face, from[0], from[1], from[2], to[0], to[1], to[2]);
    }

    public static float[] defaultUv(String face, Vector3fc from, Vector3fc to) {
        return defaultUv(face, from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
    }

    private static float[] defaultUv(String face, float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        return switch (face) {
            case "down" -> new float[]{fromX, 16f - toZ, toX, 16f - fromZ};
            case "up" -> new float[]{fromX, fromZ, toX, toZ};
            case "north" -> new float[]{16f - toX, 16f - toY, 16f - fromX, 16f - fromY};
            case "south" -> new float[]{fromX, 16f - toY, toX, 16f - fromY};
            case "west" -> new float[]{fromZ, 16f - toY, toZ, 16f - fromY};
            case "east" -> new float[]{16f - toZ, 16f - toY, 16f - fromZ, 16f - fromY};
            default -> null;
        };
    }
}
