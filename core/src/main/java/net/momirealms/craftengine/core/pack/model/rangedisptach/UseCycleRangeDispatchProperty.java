package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class UseCycleRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final float period;

    public UseCycleRangeDispatchProperty(float period) {
        this.period = period;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.USE_CYCLE;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("period", period);
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            float period = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("period", 0), "period");
            return new UseCycleRangeDispatchProperty(period);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            float period = json.has("period") ? json.get("period").getAsFloat() : 1.0f;
            return new UseCycleRangeDispatchProperty(period);
        }
    }
}
