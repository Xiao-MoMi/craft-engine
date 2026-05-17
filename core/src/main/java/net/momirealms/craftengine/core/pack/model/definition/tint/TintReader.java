package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;

import java.util.ArrayList;
import java.util.List;

public interface TintReader<T extends Tint> {

    T read(JsonObject json);

    default Either<Integer, List<Float>> parseTintValue(JsonElement element) {
        if (element instanceof JsonPrimitive jsonPrimitive) {
            return Either.left(jsonPrimitive.getAsInt());
        } else if (element instanceof JsonArray array) {
            List<Float> result = new ArrayList<>();
            for (JsonElement jsonElement : array) {
                result.add(jsonElement.getAsFloat());
            }
            return Either.right(result);
        } else if (element instanceof JsonObject object) {
            throw new IllegalArgumentException("Can't parse tint value: " + object);
        }
        return null;
    }
}
