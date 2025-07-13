package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class TimeRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String source;
    private final boolean wobble;

    public TimeRangeDispatchProperty(String source, boolean wobble) {
        this.source = source;
        this.wobble = wobble;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.TIME;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("source", source);
        if (!wobble) {
            jsonObject.addProperty("wobble", false);
        }
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            String sourceObj = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("source"), "warning.config.item.model.range_dispatch.time.missing_source");
            boolean wobble = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("wobble", true), "wobble");
            return new TimeRangeDispatchProperty(sourceObj, wobble);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            String source = json.get("source").getAsString();
            boolean wobble = !json.has("wobble") || json.get("wobble").getAsBoolean();
            return new TimeRangeDispatchProperty(source, wobble);
        }
    }
}
