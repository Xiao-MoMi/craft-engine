package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class UseCycleRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    private final int period;

    public UseCycleRangeDispatchProperty(int period) {
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
            int period = MiscUtils.getAsInt(arguments.getOrDefault("period", 0));
            return new UseCycleRangeDispatchProperty(period);
        }
    }
}
