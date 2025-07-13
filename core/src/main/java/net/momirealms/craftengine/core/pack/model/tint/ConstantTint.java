package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.Map;

public class ConstantTint implements Tint {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final Either<Integer, List<Float>> value;

    public ConstantTint(Either<Integer, List<Float>> value) {
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
            Object value = ResourceConfigUtils.requireNonNullOrThrow(ResourceConfigUtils.get(arguments, "value", "default"), "warning.config.item.model.tint.constant.missing_value");
            return new ConstantTint(parseTintValue(value));
        }
    }

    public static class Reader implements TintReader {
        @Override
        public Tint read(JsonObject json) {
            return new ConstantTint(parseTintValue(json.get("value")));
        }
    }
}
