package net.momirealms.craftengine.core.pack.model.definition.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class UsingItemConditionProperty implements ConditionProperty, LegacyModelPredicate<Boolean> {
    public static final ConditionPropertyFactory<UsingItemConditionProperty> FACTORY = new Factory();
    public static final ConditionPropertyReader<UsingItemConditionProperty> READER = new Reader();
    public static final UsingItemConditionProperty INSTANCE = new UsingItemConditionProperty();

    private UsingItemConditionProperty() {}

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "using_item");
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.SHIELD)) return "blocking";
        if (material.equals(ItemKeys.TRIDENT)) return "throwing";
        if (material.equals(ItemKeys.CROSSBOW) || material.equals(ItemKeys.BOW)) return "pulling";
        if (material.equals(ItemKeys.GOAT_HORN)) return "tooting";
        return null;
    }

    @Override
    public Number toLegacyValue(Boolean value) {
        return value ? 1 : 0;
    }

    private static class Factory implements ConditionPropertyFactory<UsingItemConditionProperty> {
        @Override
        public UsingItemConditionProperty create(ConfigSection section) {
            return INSTANCE;
        }
    }

    private static class Reader implements ConditionPropertyReader<UsingItemConditionProperty> {
        @Override
        public UsingItemConditionProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
