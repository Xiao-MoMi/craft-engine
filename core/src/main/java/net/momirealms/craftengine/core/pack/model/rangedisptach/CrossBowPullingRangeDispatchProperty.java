package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class CrossBowPullingRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    public static final CrossBowPullingRangeDispatchProperty INSTANCE = new CrossBowPullingRangeDispatchProperty();

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.CROSSBOW_PULL;
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.CROSSBOW) || material.equals(ItemKeys.BOW)) return "pull";
        return null;
    }

    @Override
    public Number toLegacyValue(Number value) {
        return value;
    }

    public static class Factory implements RangeDispatchPropertyFactory {
        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }

    public static class Reader implements RangeDispatchPropertyReader {
        @Override
        public RangeDispatchProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
