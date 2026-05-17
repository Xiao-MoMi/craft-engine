package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public final class UseCycleRangeDispatchProperty implements RangeDispatchProperty {
    public static final RangeDispatchPropertyFactory<UseCycleRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<UseCycleRangeDispatchProperty> READER = new Reader();
    private final float period;

    public UseCycleRangeDispatchProperty(float period) {
        this.period = period;
    }

    public float period() {
        return this.period;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "use_cycle");
        model.addProperty("period", this.period);
    }

    private static class Factory implements RangeDispatchPropertyFactory<UseCycleRangeDispatchProperty> {
        @Override
        public UseCycleRangeDispatchProperty create(ConfigSection section) {
            return new UseCycleRangeDispatchProperty(section.getFloat("source"));
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<UseCycleRangeDispatchProperty> {
        @Override
        public UseCycleRangeDispatchProperty read(JsonObject json) {
            return new UseCycleRangeDispatchProperty(json.has("period") ? json.get("period").getAsFloat() : 1.0f);
        }
    }
}
