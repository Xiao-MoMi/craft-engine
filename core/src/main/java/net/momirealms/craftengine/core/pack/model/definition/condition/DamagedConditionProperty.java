package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class DamagedConditionProperty implements ConditionProperty, LegacyModelPredicate<Boolean> {
    public static final ConditionPropertyFactory<DamagedConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<DamagedConditionProperty> READER = new Reader();
    public static final DamagedConditionProperty INSTANCE = new DamagedConditionProperty();

    private DamagedConditionProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "damaged");
    }

    @Override
    public String legacyPredicateId(Key material) {
        return "damaged";
    }

    @Override
    public Number toLegacyValue(Boolean value) {
        return value ? 1 : 0;
    }

    private static class Factory implements ConditionPropertyFactory<DamagedConditionProperty> {
        @Override
        public DamagedConditionProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements ConditionPropertyReader<DamagedConditionProperty> {
        @Override
        public DamagedConditionProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
