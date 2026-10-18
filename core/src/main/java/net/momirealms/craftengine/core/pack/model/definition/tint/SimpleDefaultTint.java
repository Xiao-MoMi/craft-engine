package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class SimpleDefaultTint implements Tint {
    public static final TintFactory<SimpleDefaultTint> FACTORY = new Factory();
    public static final TintReader<SimpleDefaultTint> READER = new Reader();
    private final Either<Integer, List<Float>> defaultValue;
    private final Key type;

    public SimpleDefaultTint(Key type, @Nullable Either<Integer, List<Float>> defaultValue) {
        this.defaultValue = defaultValue;
        this.type = type;
    }

    public Either<Integer, List<Float>> defaultValue() {
        return this.defaultValue;
    }

    public Key type() {
        return this.type;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.type.asMinimalString());
        applyTint(json, this.defaultValue, "default");
        return json;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SimpleDefaultTint that)) {
            return false;
        }
        return Objects.equals(this.defaultValue, that.defaultValue)
                && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.defaultValue);
        return 31 * result + Objects.hashCode(this.type);
    }

    private static class Factory implements TintFactory<SimpleDefaultTint> {
        private static final String[] DEFAULT = ConfigKeys.of("default|value");

        @Override
        public SimpleDefaultTint create(ConfigSection section) {
            return new SimpleDefaultTint(
                    section.getNonNullIdentifier("type"),
                    section.getValue(DEFAULT, Tints::getTintValue)
            );
        }
    }

    private static class Reader implements TintReader<SimpleDefaultTint> {
        @Override
        public SimpleDefaultTint read(JsonObject json) {
            return new SimpleDefaultTint(Key.of(json.get("type").getAsString()), parseTintValue(json.get("default")));
        }
    }
}
