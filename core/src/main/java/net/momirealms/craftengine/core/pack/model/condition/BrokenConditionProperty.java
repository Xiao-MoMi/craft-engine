package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class BrokenConditionProperty implements ConditionProperty, LegacyModelPredicate<Boolean> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    public static final BrokenConditionProperty INSTANCE = new BrokenConditionProperty();

    @Override
    public Key type() {
        return ConditionProperties.BROKEN;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.ELYTRA)) return "broken";
        return null;
    }

    @Override
    public Number toLegacyValue(Boolean value) {
        return value ? 1 : 0;
    }

    public static class Factory implements ConditionPropertyFactory {
        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }

    public static class Reader implements ConditionPropertyReader {
        @Override
        public ConditionProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
