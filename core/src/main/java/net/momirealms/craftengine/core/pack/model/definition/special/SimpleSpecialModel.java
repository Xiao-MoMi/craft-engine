package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import java.util.Objects;

public final class SimpleSpecialModel implements SpecialModel {
    public static final SpecialModelFactory<SimpleSpecialModel> FACTORY = new Factory();
    public static final SpecialModelReader<SimpleSpecialModel> READER = new Reader();
    private final Key type;

    public SimpleSpecialModel(Key type) {
        this.type = type;
    }

    public Key type() {
        return this.type;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.type.asMinimalString());
        return json;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SimpleSpecialModel that)) {
            return false;
        }
        return Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.type);
    }

    private static class Factory implements SpecialModelFactory<SimpleSpecialModel> {
        @Override
        public SimpleSpecialModel create(ConfigSection section) {
            return new SimpleSpecialModel(section.getNonNullIdentifier("type"));
        }
    }

    private static class Reader implements SpecialModelReader<SimpleSpecialModel> {
        @Override
        public SimpleSpecialModel read(JsonObject json) {
            return new SimpleSpecialModel(Key.of(json.get("type").getAsString()));
        }
    }
}
