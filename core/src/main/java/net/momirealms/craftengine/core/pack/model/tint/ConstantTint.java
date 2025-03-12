package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.Map;

public class ConstantTint implements Tint {
    public static final Factory FACTORY = new Factory();
    private final Either<Integer, List<Integer>> value;

    public ConstantTint(Either<Integer, List<Integer>> value) {
        this.value = value;
    }

    @Override
    public Key type() {
        return Tints.CONSTANT;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        applyAnyTint(json, value, "value");
        return json;
    }

    public static class Factory implements TintFactory {

        @Override
        public Tint create(Map<String, Object> arguments) {
            Object value = arguments.get("value");
            return new ConstantTint(parseTintValue(value));
        }
    }
}
