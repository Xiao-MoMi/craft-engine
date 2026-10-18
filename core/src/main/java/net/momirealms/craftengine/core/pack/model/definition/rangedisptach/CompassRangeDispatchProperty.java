package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import java.util.Objects;

public final class CompassRangeDispatchProperty implements RangeDispatchProperty {
    public static final RangeDispatchPropertyFactory<CompassRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<CompassRangeDispatchProperty> READER = new Reader();
    private final String target;
    private final boolean wobble;

    public CompassRangeDispatchProperty(String target, boolean wobble) {
        this.target = target;
        this.wobble = wobble;
    }

    public String target() {
        return this.target;
    }

    public boolean wobble() {
        return this.wobble;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "compass");
        model.addProperty("target", this.target);
        if (!this.wobble) {
            model.addProperty("wobble", false);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CompassRangeDispatchProperty that)) {
            return false;
        }
        return Objects.equals(this.target, that.target)
                && this.wobble == that.wobble;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.target);
        return 31 * result + Boolean.hashCode(this.wobble);
    }

    private static class Factory implements RangeDispatchPropertyFactory<CompassRangeDispatchProperty> {
        @Override
        public CompassRangeDispatchProperty create(ConfigSection section) {
            return new CompassRangeDispatchProperty(
                    section.getNonNullString("target"),
                    section.getBoolean("wobble", true)
            );
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<CompassRangeDispatchProperty> {
        @Override
        public CompassRangeDispatchProperty read(JsonObject json) {
            return new CompassRangeDispatchProperty(
                    json.get("target").getAsString(),
                    !json.has("wobble") || json.get("wobble").getAsBoolean()
            );
        }
    }
}
