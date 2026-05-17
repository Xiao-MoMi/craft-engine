package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;

import java.util.List;
import java.util.function.Supplier;

public interface Tint extends Supplier<JsonObject> {

    default void applyTint(JsonObject json, Either<Integer, List<Float>> value, String key) {
        if (value == null) {
            json.addProperty(key, 16777215);
        } else {
            value.ifLeft(i -> {
                json.addProperty(key, i);
            }).ifRight(list -> {
                JsonArray array = new JsonArray();
                for (float i : list) {
                    array.add(i);
                }
                json.add(key, array);
            });
        }
    }
}
