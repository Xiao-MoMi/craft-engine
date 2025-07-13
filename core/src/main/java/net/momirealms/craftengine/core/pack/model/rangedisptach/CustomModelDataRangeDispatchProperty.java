package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class CustomModelDataRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final int index;

    public CustomModelDataRangeDispatchProperty(int index) {
        this.index = index;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.CUSTOM_MODEL_DATA;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("index", index);
    }

    @Override
    public String legacyPredicateId(Key material) {
        return "custom_model_data";
    }

    @Override
    public Number toLegacyValue(Number value) {
        return value.intValue();
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            int index = ResourceConfigUtils.getAsInt(arguments.getOrDefault("index", 0), "index");
            return new CustomModelDataRangeDispatchProperty(index);
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            int index = json.has("index") ? json.get("index").getAsInt() : 0;
            return new CustomModelDataRangeDispatchProperty(index);
        }
    }
}
