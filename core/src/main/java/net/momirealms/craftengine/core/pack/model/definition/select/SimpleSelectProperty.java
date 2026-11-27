package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import java.util.Objects;

public final class SimpleSelectProperty implements SelectProperty {
    public static final SelectPropertyFactory<SimpleSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<SimpleSelectProperty> READER = new Reader();
    private final Key type;

    public SimpleSelectProperty(Key type) {
        this.type = type;
    }

    public Key type() {
        return this.type;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", this.type.asMinimalString());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SimpleSelectProperty that)) {
            return false;
        }
        return Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.type);
    }

    private static class Factory implements SelectPropertyFactory<SimpleSelectProperty> {
        @Override
        public SimpleSelectProperty create(ConfigSection section) {
            return new SimpleSelectProperty(section.getNonNullIdentifier("property"));
        }
    }

    private static class Reader implements SelectPropertyReader<SimpleSelectProperty> {
        @Override
        public SimpleSelectProperty read(JsonObject json) {
            return new SimpleSelectProperty(Key.of(json.get("property").getAsString()));
        }
    }
}
