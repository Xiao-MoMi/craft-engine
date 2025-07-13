package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class ChargeTypeSelectProperty implements SelectProperty, LegacyModelPredicate<String> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    public static final ChargeTypeSelectProperty INSTANCE = new ChargeTypeSelectProperty();

    @Override
    public Key type() {
        return SelectProperties.CHARGE_TYPE;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.CROSSBOW)) return "firework";
        return null;
    }

    @Override
    public Number toLegacyValue(String value) {
        if (value.equals("rocket")) return 1;
        return 0;
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
