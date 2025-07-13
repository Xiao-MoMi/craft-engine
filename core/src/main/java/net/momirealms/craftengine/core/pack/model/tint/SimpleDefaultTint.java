package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.type.Either;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SimpleDefaultTint implements Tint {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final Either<Integer, List<Float>> defaultValue;
    private final Key type;

    public SimpleDefaultTint(Key type, @Nullable Either<Integer, List<Float>> defaultValue) {
        this.defaultValue = defaultValue;
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
        applyAnyTint(json, this.defaultValue, "default");
        return json;
    }

    public static class Factory implements TintFactory {
        @Override
        public Tint create(Map<String, Object> arguments) {
            Object value = arguments.containsKey("default") ? arguments.getOrDefault("default", 0) : arguments.getOrDefault("value", 0);
            Key type = Key.of(arguments.get("type").toString());
            return new SimpleDefaultTint(type, parseTintValue(value));
        }
    }

    public static class Reader implements TintReader {
        @Override
        public Tint read(JsonObject json) {
            return new SimpleDefaultTint(Key.of(json.get("type").getAsString()), parseTintValue(json.get("default")));
        }
    }
}
