package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class CompassRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String target;
    private final boolean wobble;

    public CompassRangeDispatchProperty(String target, boolean wobble) {
        this.target = target;
        this.wobble = wobble;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.COMPASS;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("target", this.target);
        if (!this.wobble) {
            jsonObject.addProperty("wobble", false);
        }
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            String targetObj = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("target"), "warning.config.item.model.range_dispatch.compass.missing_target");
            boolean wobble = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("wobble", true), "wobble");
            return new CompassRangeDispatchProperty(targetObj, wobble);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            String target = json.get("target").getAsString();
            boolean wobble = !json.has("wobble") || json.get("wobble").getAsBoolean();
            return new CompassRangeDispatchProperty(target, wobble);
        }
    }
}
