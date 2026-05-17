package net.momirealms.craftengine.core.util;

import com.google.gson.JsonArray;
import org.joml.Vector3f;

public final class VectorUtils {
    private VectorUtils() {}

    public static Vector3f vector3f(JsonArray array) {
        if (array == null || array.size() != 3) {
            return new Vector3f();
        }
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public static JsonArray toJson(Vector3f vector3f) {
        JsonArray array = new JsonArray();
        array.add(vector3f.x());
        array.add(vector3f.y());
        array.add(vector3f.z());
        return array;
    }
}
