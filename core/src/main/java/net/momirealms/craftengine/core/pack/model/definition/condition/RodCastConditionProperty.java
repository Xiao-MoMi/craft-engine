package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class RodCastConditionProperty implements ConditionProperty, LegacyModelPredicate<Boolean> {
    public static final ConditionPropertyFactory<RodCastConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<RodCastConditionProperty> READER = new Reader();
    public static final RodCastConditionProperty INSTANCE = new RodCastConditionProperty();

    private RodCastConditionProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "fishing_rod/cast");
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.FISHING_ROD)) return "cast";
        return null;
    }

    @Override
    public Number toLegacyValue(Boolean value) {
        return value ? 1 : 0;
    }

    private static class Factory implements ConditionPropertyFactory<RodCastConditionProperty> {
        @Override
        public RodCastConditionProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements ConditionPropertyReader<RodCastConditionProperty> {
        @Override
        public RodCastConditionProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
