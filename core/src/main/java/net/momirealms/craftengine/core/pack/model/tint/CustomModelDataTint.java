package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.Map;

public class CustomModelDataTint implements Tint {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final Either<Integer, List<Float>> value;
    private final int index;

    public CustomModelDataTint(Either<Integer, List<Float>> value, int index) {
        this.index = index;
        this.value = value;
    }

    @Override
    public Key type() {
        return Tints.CUSTOM_MODEL_DATA;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        if (index != 0)
            json.addProperty("index", this.index);
        applyAnyTint(json, this.value, "default");
        return json;
    }

    public static class Factory implements TintFactory {
        @Override
        public Tint create(Map<String, Object> arguments) {
            Object value = arguments.containsKey("default") ? arguments.getOrDefault("default", 0) : arguments.getOrDefault("value", 0);
            int index = ResourceConfigUtils.getAsInt(arguments.getOrDefault("index", 0), "index");
            return new CustomModelDataTint(parseTintValue(value), index);
        }
    }

    public static class Reader implements TintReader {
        @Override
        public Tint read(JsonObject json) {
            return new CustomModelDataTint(
                    parseTintValue(json.get("default")),
                    json.has("index") ? json.get("index").getAsInt() : 0
            );
        }
    }
}
