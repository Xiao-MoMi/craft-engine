package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class CustomModelDataRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final RangeDispatchPropertyFactory<CustomModelDataRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<CustomModelDataRangeDispatchProperty> READER = new Reader();
    private final int index;

    public CustomModelDataRangeDispatchProperty(int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "custom_model_data");
        model.addProperty("index", this.index);
    }

    @Override
    public String legacyPredicateId(Key material) {
        return "custom_model_data";
    }

    @Override
    public Number toLegacyValue(Number value) {
        return value.intValue();
    }

    private static class Factory implements RangeDispatchPropertyFactory<CustomModelDataRangeDispatchProperty> {
        @Override
        public CustomModelDataRangeDispatchProperty create(ConfigSection section) {
            return new CustomModelDataRangeDispatchProperty(section.getInt("index"));
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<CustomModelDataRangeDispatchProperty> {
        @Override
        public CustomModelDataRangeDispatchProperty read(JsonObject json) {
            return new CustomModelDataRangeDispatchProperty(json.has("index") ? json.get("index").getAsInt() : 0);
        }
    }
}
