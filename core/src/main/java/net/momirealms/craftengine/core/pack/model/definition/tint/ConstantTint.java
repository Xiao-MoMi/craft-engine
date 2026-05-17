package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.List;

public final class ConstantTint implements Tint {
    public static final TintFactory<ConstantTint> FACTORY = new Factory();
    public static final TintReader<ConstantTint> READER = new Reader();
    private final Either<Integer, List<Float>> value;

    public ConstantTint(Either<Integer, List<Float>> value) {
        this.value = value;
    }

    public Either<Integer, List<Float>> value() {
        return this.value;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "constant");
        applyTint(json, this.value, "value");
        return json;
    }

    private static class Factory implements TintFactory<ConstantTint> {
        private static final String[] VALUE = new String[] {"value", "default"};

        @Override
        public ConstantTint create(ConfigSection section) {
            return new ConstantTint(section.getValue(VALUE, Tints::getTintValue));
        }
    }

    private static class Reader implements TintReader<ConstantTint> {
        @Override
        public ConstantTint read(JsonObject json) {
            return new ConstantTint(parseTintValue(json.get("value")));
        }
    }
}
