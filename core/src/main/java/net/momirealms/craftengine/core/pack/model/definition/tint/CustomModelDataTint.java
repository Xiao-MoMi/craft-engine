package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CustomModelDataTint that)) {
            return false;
        }
        return Objects.equals(this.value, that.value)
                && this.index == that.index;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.value);
        return 31 * result + Integer.hashCode(this.index);
    }

    private static class Factory implements TintFactory<CustomModelDataTint> {
        private static final String[] DEFAULT = ConfigKeys.of("default|value");

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
