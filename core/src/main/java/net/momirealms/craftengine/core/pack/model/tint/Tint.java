package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.function.Supplier;

public interface Tint extends Supplier<JsonObject> {

    Key type();

    default void applyAnyTint(JsonObject json, Either<Integer, List<Float>> value, String key) {
        if (value == null) return;
        if (value.primary().isPresent()) {
            json.addProperty(key, value.primary().get());
        } else {
            List<Float> list = value.fallback().get();
            if (list.size() != 3) {
                throw new RuntimeException("Invalid tint value list size: " + list.size() + " which is expected to be 3");
            }
            JsonArray array = new JsonArray();
            for (float i : list) {
                array.add(i);
            }
            json.add(key, array);
        }
    }
}
