package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.incendo.cloud.type.Either;

import java.util.ArrayList;
import java.util.List;

public interface TintReader {

    Tint read(JsonObject json);

    default Either<Integer, List<Float>> parseTintValue(JsonElement element) {
        if (element instanceof JsonPrimitive jsonPrimitive) {
            return Either.ofPrimary(jsonPrimitive.getAsInt());
        } else if (element instanceof JsonArray array) {
            List<Float> result = new ArrayList<>();
            for (JsonElement jsonElement : array) {
                result.add(jsonElement.getAsFloat());
            }
            return Either.ofFallback(result);
        } else if (element instanceof JsonObject object) {
            throw new IllegalArgumentException("Can't parse tint value: " + object);
        }
        return null;
    }
}
