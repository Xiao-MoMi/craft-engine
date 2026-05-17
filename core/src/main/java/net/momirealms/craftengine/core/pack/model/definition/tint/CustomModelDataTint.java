package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.List;

public final class CustomModelDataTint implements Tint {
    public static final TintFactory<CustomModelDataTint> FACTORY = new Factory();
    public static final TintReader<CustomModelDataTint> READER = new Reader();
    private final Either<Integer, List<Float>> value;
    private final int index;

    public CustomModelDataTint(Either<Integer, List<Float>> value, int index) {
        this.index = index;
        this.value = value;
    }

    public Either<Integer, List<Float>> value() {
        return this.value;
    }

    public int index() {
        return this.index;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "custom_model_data");
        if (this.index != 0)
            json.addProperty("index", this.index);
        applyTint(json, this.value, "default");
        return json;
    }

    private static class Factory implements TintFactory<CustomModelDataTint> {
        private static final String[] DEFAULT = new String[]{"default", "value"};

        @Override
        public CustomModelDataTint create(ConfigSection section) {
            return new CustomModelDataTint(
                    section.getValue(DEFAULT, Tints::getTintValue),
                    section.getInt("index")
            );
        }
    }

    private static class Reader implements TintReader<CustomModelDataTint> {
        @Override
        public CustomModelDataTint read(JsonObject json) {
            return new CustomModelDataTint(
                    parseTintValue(json.get("default")),
                    json.has("index") ? json.get("index").getAsInt() : 0
            );
        }
    }
}
