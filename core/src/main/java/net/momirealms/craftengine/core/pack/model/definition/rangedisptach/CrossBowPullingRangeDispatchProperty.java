package net.momirealms.craftengine.core.pack.model.definition.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class CrossBowPullingRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Number> {
    public static final RangeDispatchPropertyFactory<CrossBowPullingRangeDispatchProperty> FACTORY = new Factory();
    public static final RangeDispatchPropertyReader<CrossBowPullingRangeDispatchProperty> READER = new Reader();
    public static final CrossBowPullingRangeDispatchProperty INSTANCE = new CrossBowPullingRangeDispatchProperty();

    private CrossBowPullingRangeDispatchProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "crossbow/pull");
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

    private static class Factory implements RangeDispatchPropertyFactory<CrossBowPullingRangeDispatchProperty> {
        @Override
        public CrossBowPullingRangeDispatchProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements RangeDispatchPropertyReader<CrossBowPullingRangeDispatchProperty> {
        @Override
        public CrossBowPullingRangeDispatchProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
