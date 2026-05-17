package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

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
