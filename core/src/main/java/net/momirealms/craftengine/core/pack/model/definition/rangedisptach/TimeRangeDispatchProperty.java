package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public final class TimeRangeDispatchProperty implements RangeDispatchProperty {
    public static final RangeDispatchPropertyFactory<TimeRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<TimeRangeDispatchProperty> READER = new Reader();
    private final String source;
    private final boolean wobble;

    public TimeRangeDispatchProperty(String source, boolean wobble) {
        this.source = source;
        this.wobble = wobble;
    }

    public String source() {
        return this.source;
    }

    public boolean wobble() {
        return this.wobble;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "time");
        model.addProperty("source", this.source);
        if (!this.wobble) {
            model.addProperty("wobble", false);
        }
    }

    private static class Factory implements RangeDispatchPropertyFactory<TimeRangeDispatchProperty> {
        @Override
        public TimeRangeDispatchProperty create(ConfigSection section) {
            return new TimeRangeDispatchProperty(
                    section.getNonNullString("source"),
                    section.getBoolean("wobble", true)
            );
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<TimeRangeDispatchProperty> {
        @Override
        public TimeRangeDispatchProperty read(JsonObject json) {
            return new TimeRangeDispatchProperty(
                    json.get("source").getAsString(),
                    !json.has("wobble") || json.get("wobble").getAsBoolean()
            );
        }
    }
}
