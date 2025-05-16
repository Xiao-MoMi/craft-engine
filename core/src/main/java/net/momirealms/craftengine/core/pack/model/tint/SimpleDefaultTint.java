package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.Map;

public class SimpleDefaultTint implements Tint {
    public static final Factory FACTORY = new Factory();
    private final Either<Integer, List<Float>> value;
    private final Key type;

    public SimpleDefaultTint(Either<Integer, List<Float>> value, Key type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public Key type() {
        return type;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        applyAnyTint(json, value, "default");
        return json;
    }

    public static class Factory implements TintFactory {

        @Override
        public Tint create(Map<String, Object> arguments) {
            Object value = arguments.getOrDefault("default", 0);
            Key type = Key.of(arguments.get("type").toString());
            return new SimpleDefaultTint(parseTintValue(value), type);
        }
    }
}
