package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import java.util.Objects;

public final class SimpleRangeDispatchProperty implements RangeDispatchProperty {
    public static final RangeDispatchPropertyFactory<SimpleRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<SimpleRangeDispatchProperty> READER = new Reader();
    private final Key type;

    public SimpleRangeDispatchProperty(Key type) {
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
        if (!(object instanceof SimpleRangeDispatchProperty that)) {
            return false;
        }
        return Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.type);
    }

    private static class Factory implements RangeDispatchPropertyFactory<SimpleRangeDispatchProperty> {
        @Override
        public SimpleRangeDispatchProperty create(ConfigSection section) {
            return new SimpleRangeDispatchProperty(section.getNonNullIdentifier("property"));
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<SimpleRangeDispatchProperty> {
        @Override
        public SimpleRangeDispatchProperty read(JsonObject json) {
            return new SimpleRangeDispatchProperty(Key.of(json.get("property").getAsString()));
        }
    }
}
