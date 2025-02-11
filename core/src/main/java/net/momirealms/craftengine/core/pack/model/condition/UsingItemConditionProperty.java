package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class UsingItemConditionProperty implements ConditionProperty, LegacyModelPredicate<Boolean> {
    public static final Factory FACTORY = new Factory();

    @Override
    public Key type() {
        return ConditionProperties.USING_ITEM;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.SHIELD)) return "blocking";
        if (material.equals(ItemKeys.TRIDENT)) return "throwing";
        if (material.equals(ItemKeys.CROSSBOW) || material.equals(ItemKeys.BOW)) return "pulling";
        if (material.equals(ItemKeys.GOAT_HORN)) return "tooting";
        throw new IllegalArgumentException("Unsupported material: " + material);
    }

    @Override
    public Number toLegacyValue(Boolean value) {
        return value ? 1 : 0;
    }

    public static class Factory implements ConditionPropertyFactory {
        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            return new UsingItemConditionProperty();
        }
    }
}
