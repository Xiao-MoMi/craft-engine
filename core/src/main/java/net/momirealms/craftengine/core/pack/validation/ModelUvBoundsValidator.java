package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModelUvBoundsValidator {
    private static final float MIN_UV = 0f;
    private static final float MAX_UV = 16f;

    private ModelUvBoundsValidator() {
    }

    public static List<Problem> validate(JsonObject model, Map<String, Key> textures) {
        if (!(model.get("elements") instanceof JsonArray elements)) {
            return List.of();
        }

        List<Problem> problems = new ArrayList<>();
        for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
            if (!(elements.get(elementIndex) instanceof JsonObject element)) {
                continue;
            }

            float[] from = readVector(element, "from");
            float[] to = readVector(element, "to");
            if (from == null || to == null || !(element.get("faces") instanceof JsonObject faces)) {
                continue;
            }

            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                if (!(entry.getValue() instanceof JsonObject face) || !isRenderable(entry.getKey(), from, to)) {
                    continue;
                }

                float[] uv = face.has("uv")
                        ? readUv(face)
                        : defaultUv(entry.getKey(), from, to);
                if (uv == null || !isOutOfBounds(uv)) {
                    continue;
                }

                problems.add(new Problem(
                        elementIndex,
                        entry.getKey(),
                        resolveTexture(face, textures),
                        uv[0], uv[1], uv[2], uv[3]
                ));
            }
        }
        return problems;
    }

    public static int fix(JsonObject model, List<Problem> problems) {
        if (!(model.get("elements") instanceof JsonArray elements)) {
            return 0;
        }

        int fixed = 0;
        for (Problem problem : problems) {
            if (problem.elementIndex() < 0 || problem.elementIndex() >= elements.size()
                    || !(elements.get(problem.elementIndex()) instanceof JsonObject element)
                    || !(element.get("faces") instanceof JsonObject faces)
                    || !(faces.get(problem.face()) instanceof JsonObject face)) {
                continue;
            }

            JsonArray uv = new JsonArray();
            uv.add(clamp(problem.u1()));
            uv.add(clamp(problem.v1()));
            uv.add(clamp(problem.u2()));
            uv.add(clamp(problem.v2()));
            face.add("uv", uv);
            fixed++;
        }
        return fixed;
    }

    private static float[] readVector(JsonObject object, String key) {
        if (!(object.get(key) instanceof JsonArray array) || array.size() != 3) {
            return null;
        }
        return readFiniteFloats(array, 3);
    }

    private static float[] readUv(JsonObject face) {
        if (!(face.get("uv") instanceof JsonArray array)) {
            return null;
        }
        if (array.size() != 4) {
            return null;
        }
        return readFiniteFloats(array, 4);
    }

    private static float[] readFiniteFloats(JsonArray array, int size) {
        float[] values = new float[size];
        try {
            for (int i = 0; i < size; i++) {
                float value = array.get(i).getAsFloat();
                if (!Float.isFinite(value)) {
                    return null;
                }
                values[i] = value;
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return values;
    }

    private static boolean isRenderable(String face, float[] from, float[] to) {
        return switch (face) {
            case "down", "up" -> from[0] != to[0] && from[2] != to[2];
            case "north", "south" -> from[0] != to[0] && from[1] != to[1];
            case "west", "east" -> from[1] != to[1] && from[2] != to[2];
            default -> false;
        };
    }

    private static float[] defaultUv(String face, float[] from, float[] to) {
        return switch (face) {
            case "down" -> new float[]{from[0], 16f - to[2], to[0], 16f - from[2]};
            case "up" -> new float[]{from[0], from[2], to[0], to[2]};
            case "north" -> new float[]{16f - to[0], 16f - to[1], 16f - from[0], 16f - from[1]};
            case "south" -> new float[]{from[0], 16f - to[1], to[0], 16f - from[1]};
            case "west" -> new float[]{from[2], 16f - to[1], to[2], 16f - from[1]};
            case "east" -> new float[]{16f - to[2], 16f - to[1], 16f - from[2], 16f - from[1]};
            default -> null;
        };
    }

    private static boolean isOutOfBounds(float[] uv) {
        float minU = Math.min(uv[0], uv[2]);
        float minV = Math.min(uv[1], uv[3]);
        float maxU = Math.max(uv[0], uv[2]);
        float maxV = Math.max(uv[1], uv[3]);
        return minU < MIN_UV || minV < MIN_UV || maxU > MAX_UV || maxV > MAX_UV;
    }

    private static float clamp(float value) {
        return Math.max(MIN_UV, Math.min(MAX_UV, value));
    }

    private static String resolveTexture(JsonObject face, Map<String, Key> textures) {
        if (!(face.get("texture") instanceof JsonPrimitive texturePrimitive)) {
            return "<missing>";
        }

        String texture = texturePrimitive.getAsString();
        if (!texture.startsWith("#")) {
            return texture;
        }

        Key resolved = textures.get(texture.substring(1));
        return resolved == null ? texture : resolved.asString();
    }

    public record Problem(int elementIndex, String face, String texture,
                          float u1, float v1, float u2, float v2) {

        public String uv() {
            return "[" + u1 + ", " + v1 + ", " + u2 + ", " + v2 + "]";
        }
    }
}
